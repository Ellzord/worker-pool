package com.pirum.exercises.worker

import monix.execution.Scheduler.Implicits.global

import scala.concurrent.duration.{DurationInt, FiniteDuration}

object Main extends App with Program {

  def program(tasks: List[Task], timeout: FiniteDuration, workers: Int): Unit = {
    // uses the default scheduler (processor count [[scala.concurrent.ExecutionContext.global]])
    val result = TaskRunner.execute(tasks, timeout, workers).runSyncUnsafe() // :(
    // forgive the printlns
    println(s"actions = $tasks")
    println(s"timeout = $timeout")
    println(s"workers = $workers")
    println(s"result.successful = ${result.successful}")
    println(s"result.failed = ${result.failed}")
    println(s"result.timedOut = ${result.timedOut}")
  }

  // example 1
  program(List(
    FailingTask(3.seconds),
    DelayedTask(4.seconds),
    DelayedTask(2.seconds),
    FailingTask(1.seconds)
  ), 8.seconds, 4)

  // example 2
  program(List(
    FailingTask(3.seconds),
    HangingTask,
    DelayedTask(4.seconds),
    DelayedTask(2.seconds),
    DelayedTask(1.seconds)
  ), 8.seconds, 4)

  println("Good luck 🤓") // thanks
}
