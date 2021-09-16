package com.pirum.exercises.worker

import monix.execution.schedulers.TestScheduler
import org.scalatest.concurrent.TimeLimits.failAfter
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers._
import org.scalatest.time.{Millis, Span}

import scala.concurrent.duration.DurationInt
import scala.util.Success

class TaskSpec extends AnyFreeSpec with must.Matchers {

  "TaskRunner" - {

    implicit val testScheduler: TestScheduler = TestScheduler()

    "must order tasks" in {

      val task1 = DelayedTask(50.milliseconds)
      val task2 = DelayedTask(60.milliseconds)
      val task3 = FailingTask(70.milliseconds)
      val task4 = FailingTask(80.milliseconds)
      val task5 = HangingTask
      val task6 = HangingTask

      val f = TaskRunner.execute(List(task4, task5, task2, task1, task3, task6), 150.milliseconds, 4).runToFuture
      testScheduler.tick(200.milliseconds)

      f.value mustBe Some(Success(TaskResults(List(task1, task2), List(task3, task4), List(task5, task6))))
    }

    "must time out tasks" in {

      val task1 = DelayedTask(75.milliseconds)
      val task2 = FailingTask(75.milliseconds)
      val task3 = HangingTask

      val f = TaskRunner.execute(List(task1, task2, task3), 50.milliseconds, 3).runToFuture
      testScheduler.tick(100.milliseconds)

      f.value mustBe Some(Success(TaskResults(Nil, Nil, List(task1, task2, task3))))
    }

    "must restrict work to worker count" in {

      val task1 = HangingTask
      val task2 = FailingTask(30.milliseconds)

      val f = TaskRunner.execute(List(task1, task2), 50.milliseconds, 1).runToFuture
      testScheduler.tick(100.milliseconds)

      f.value mustBe Some(Success(TaskResults(Nil, Nil, List(task1, task2))))
    }

    "must return in a timely manner" in {

      val task1 = DelayedTask(10.milliseconds)

      val f = TaskRunner.execute(List(task1), 20.milliseconds, 1).runToFuture

      failAfter(Span(30, Millis)) {

        testScheduler.tick(20.milliseconds)

        f.value mustBe Some(Success(TaskResults(List(task1), Nil, Nil)))
      }
    }
  }
}
