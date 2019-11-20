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

package at.sbaresearch.mqtt4android.analysis;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Sleeper;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.function.Consumer;

@Slf4j
@Component
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LogoutWatchdog {

  public static final long POLL_INTERVAL = 250L;
  WebDriver webDriver;

  /**
   * this runs a new thread for every call!
   */
  public void onLogout(Consumer<Void> handler) {
    new Thread(new LogoutRunner(webDriver, handler), "logoutWatchdog").start();
  }

  @Slf4j
  @AllArgsConstructor
  @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
  public static class LogoutRunner implements Runnable {
    WebDriver webDriver;
    Consumer<Void> handler;

    @Override
    public void run() {
      val ignored = 100L;

      val logoutWait = new WebDriverWait(
          webDriver,
          java.time.Clock.systemDefaultZone(),
          Sleeper.SYSTEM_SLEEPER,
          ignored,
          POLL_INTERVAL)
          .withTimeout(Duration.ofDays(1));

      // wait till the end of the world
      while (true) {
        logoutWait.until(ExpectedConditions.urlContains("expired"));
        synchronized (LogoutWatchdog.class) {
          log.info("logout occurred");
          handler.accept(null);
        }
      }
    }
  }
}
