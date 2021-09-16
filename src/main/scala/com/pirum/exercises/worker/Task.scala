package com.pirum.exercises.worker

import cats.implicits._
import monix.eval.{Task => MonixTask}

import java.util.concurrent.{TimeUnit, TimeoutException}
import scala.concurrent.duration._
import scala.util.control.NonFatal

// A task that either succeeds after n seconds, fails after n seconds, or never terminates
sealed trait Task {
  def execute: MonixTask[Unit]
}

final case class DelayedTask(duration: FiniteDuration) extends Task { // succeeds
  override def execute: MonixTask[Unit] = MonixTask.unit.delayExecution(duration)
}

final case class FailingTask(duration: FiniteDuration) extends Task { // fails
  override def execute: MonixTask[Unit] = MonixTask.raiseError(new Exception("BANG")).delayExecution(duration)
}

object HangingTask extends Task { // never completes
  override def execute: MonixTask[Unit] = MonixTask.never
  override val toString: String = "HangingTask"
}

final case class TaskResults(successful: List[Task], failed: List[Task], timedOut: List[Task])

object TaskRunner {

  private object ExecutionOutcome extends Enumeration {
    type ExecutionOutcome = Value
    val Succeeded, Failed, Hung = Value
  }

  import ExecutionOutcome.ExecutionOutcome

  private final case class Wrapper(task: Task, durationMs: Long, outcome: ExecutionOutcome)

  def execute(tasks: List[Task], timeout: FiniteDuration, workers: Int): MonixTask[TaskResults] =
    MonixTask.deferAction { sc =>
      val start = sc.clockMonotonic(TimeUnit.MILLISECONDS)
      MonixTask.parSequenceN(workers)(tasks.map { task =>
        MonixTask.defer {
          val now = sc.clockMonotonic(TimeUnit.MILLISECONDS)
          task
            .execute
            .as(ExecutionOutcome.Succeeded) // not interested in the actual result
            .onErrorRecover({ case NonFatal(_) => ExecutionOutcome.Failed }) // catch failing tasks
            .map(Wrapper(task, sc.clockMonotonic(TimeUnit.MILLISECONDS) - now, _)) // execution time (not including in queue)
            .timeout(timeout.minus((now - start).milliseconds)) // remaining timeout based on queue time
        }
        .onErrorRecover({ case _: TimeoutException => Wrapper(task, 0, ExecutionOutcome.Hung) }) // task timed out
//        .flatTap { wrappedTask => MonixTask(println(s"$wrappedTask ${Thread.currentThread.getName}")) } // ** left here if you are interested
      }).map { wrappedTasks =>

        val orderedWrappedTasks = wrappedTasks.sortBy(_.durationMs)
        TaskResults(
          successful = filterAndUnwrap(orderedWrappedTasks, ExecutionOutcome.Succeeded),
          failed = filterAndUnwrap(orderedWrappedTasks, ExecutionOutcome.Failed),
          timedOut = filterAndUnwrap(orderedWrappedTasks, ExecutionOutcome.Hung)
        )
      }
    }

  private def filterAndUnwrap(wrappedTasks: List[Wrapper], outcome: ExecutionOutcome): List[Task] =
    wrappedTasks.collect({ case wrappedTask if wrappedTask.outcome == outcome => wrappedTask.task })
}