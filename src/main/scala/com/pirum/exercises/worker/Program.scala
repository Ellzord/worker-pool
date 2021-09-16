package com.pirum.exercises.worker

import scala.concurrent.duration.FiniteDuration

trait Program {
  // ** wasn't sure if i was allowed to edit this - without editing main isn't really suitable for testing (other than mocking if you add dependency injection)
  def program(tasks: List[Task], timeout: FiniteDuration, workers: Int): Unit
}