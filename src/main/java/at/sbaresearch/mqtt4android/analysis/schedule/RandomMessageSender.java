/*
 * Copyright (c) 2020 Harald Jagenteufel.
 *
 * This file is part of wire-analysis.
 *
 *     wire-analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     wire-analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with wire-analysis.  If not, see <https://www.gnu.org/licenses/>.
 */

package at.sbaresearch.mqtt4android.analysis.schedule;

import at.sbaresearch.mqtt4android.analysis.actions.ConverstationActions;
import at.sbaresearch.mqtt4android.analysis.statistics.Statistics;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.time.Instant;
import java.util.Random;

@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RandomMessageSender {

  ConverstationActions converstationActions;
  SleepManager sleepManager;
  Statistics statistics;
  int min;
  int max;
  @NonFinal
  long count = 0;

  public void start(Random random) {
    count = 0;
    while(true) {
      sendAndSleep(random);
    }
  }

  public void start(Random random, Instant until) {
    count = 0;
    while(Instant.now().isBefore(until)) {
      sendAndSleep(random);
    }
    log.info("run finished, timelimit reached");
  }

  private void sendAndSleep(Random random) {
    converstationActions.sendMessage(count);
    statistics.messageSent(count);
    count++;

    sleep(random, min, max);
  }

  private void sleep(Random random, int min, int max) {
    if (min >= max) {
      throw new IllegalArgumentException("min must be smaller than max");
    }
    val sleepTime = random.nextInt(max - min) + min;
    sleepManager.sleep(sleepTime);
  }

}
