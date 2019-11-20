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

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SleepManager {

  public void sleep(int seconds) {
    log.info("sleeping for {}s", seconds);
    val durationMillis = seconds * 1000L;
    sleepMillis(durationMillis);
  }

  private void sleepMillis(long milliseconds) {
    val startMilis = System.currentTimeMillis();
    try {
      Thread.sleep(milliseconds);
    } catch (InterruptedException e) {
      log.warn("sleeping interrupted, reschedule");
      val cur = System.currentTimeMillis();
      sleepMillis(milliseconds - (cur - startMilis));
    }
  }

}
