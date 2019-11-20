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
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Sleeper;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;

import javax.annotation.RegEx;
import java.time.Duration;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DeliveredWatchdog {

  public static final long POLL_INTERVAL = 1000L;
  WebDriver webDriver;

  /**
   * this runs a new thread for every call!
   */
  public void onDelivered(Consumer<Long> handler) {
    new Thread(new Runner(webDriver, handler), "deliveredWatchdog").start();
  }

  @Slf4j
  @RequiredArgsConstructor
  @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
  public static class Runner implements Runnable {
    WebDriver driver;
    Consumer<Long> handler;
    Pattern msgIdParser = Pattern.compile("message (\\d+)");

    @Override
    public void run() {
      // wait till the end of the world
      while (true) {
        try {
          val messageWithLastDeliverd = driver.findElements(By.xpath(
              "//div[@id='message-list']" +
                  "//div[@class='message-body']" +
                  "/div[@class='message-body-actions']" +
                  "/span[@class='message-status' and text()='Delivered']" +
                  "/parent::*" +
                  "/parent::*" +
                  "/div[@class='text']"));
          if (messageWithLastDeliverd.size() > 0) {
            val text = messageWithLastDeliverd.get(0).getText();
            log.debug("last message delivered: {}", text);

            Matcher matcher = msgIdParser.matcher(text);
            if (matcher.matches()) {
              val idStr = matcher.group(1);
              val id = Long.parseLong(idStr);
              log.debug("last message delivered parsed: {}", id);
              handler.accept(id);
            }
          } else {
            log.info("no last message delivered found");
          }

          try {
            Thread.sleep(POLL_INTERVAL);
          } catch (InterruptedException ignored) {
          }
        } catch (Exception e) {
          log.warn("exception in deliveredWatchdog {}", e.getMessage());
        }
      }
    }
  }
}
