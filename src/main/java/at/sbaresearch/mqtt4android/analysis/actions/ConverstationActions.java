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

package at.sbaresearch.mqtt4android.analysis.actions;

import at.sbaresearch.mqtt4android.analysis.LogoutWatchdog;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ConverstationActions {
  public static final int MAX_RETRIES = 20;

  WebDriverWait wait;
  WebDriver driver;
  ActionHelper helper;
  String hostname;
  String conversationPartner;

  public ConverstationActions(WebDriverWait wait, WebDriver driver, ActionHelper helper,
    @Value("${web.hostname}") String hostname,
      @Value("${conversation.partner}") String conversationPartner) {
      this.wait = wait;
    this.driver = driver;
    this.helper = helper;
    this.hostname = hostname;
    this.conversationPartner = conversationPartner;
  }

  /**
   * must be executed once before sending messages
   */
  public void loadConversation() {
    log.info("loading conversation");
    driver.get(hostname + "/");
    // TODO load a specific conversation
  }

  public void deleteAndReloadConversation() {
    log.info("deleting conv history");

    By contextMenu = By.cssSelector(".conversation-list-cell-context-menu");
    try {
      waitAndClick(contextMenu);
      By deleteAction = By.cssSelector(".ctx-menu-item:nth-child(3)");
      waitAndClick(deleteAction);
      By confirmBtn = By.cssSelector(".modal__button--primary");
      waitAndClick(confirmBtn);
    } catch (TimeoutException ex) {
      log.warn("cannot delete history, no conversation present?");
    }

    try {
      Thread.sleep(100L);
    } catch (InterruptedException ignored) {
    }
    log.info("loading conv by search");
    By searchBtn = By.cssSelector(".button-icon-large:nth-child(1) > svg");
    waitAndClick(searchBtn);

    By searchInputBy = By.cssSelector(".search-input");
    waitAndClick(searchInputBy);
    driver.findElement(searchInputBy).sendKeys(conversationPartner);

    wait.until(ExpectedConditions
        .presenceOfElementLocated(By.className("participant-item-content-name")));

    driver.findElement(searchInputBy).sendKeys(Keys.ENTER);
    log.info("loading conv by search - done");
  }

  private void waitAndClick(By elementBy) {
    helper.blockingWaitForElement(elementBy);
    wait.until(ExpectedConditions.elementToBeClickable(elementBy));
    driver.findElement(elementBy).click();
  }

  public void sendMessage(long number) {
    log.info("sending message {}", number);

    var sent = false;
    var failCount = 0;

    do {
      try {
        synchronized (LogoutWatchdog.class) {
          sent = doSend(number);
        }
      } catch (WebDriverException ex) {
        log.warn("sending of message {} interrupted", number);
        failCount++;

        if (failCount > MAX_RETRIES) {
          log.error("sending of message {} aborted, too many retries: {}", number, failCount);
          throw new CannotSendException("failed to send after " + failCount + " retries");
        }

        sleep(LogoutWatchdog.POLL_INTERVAL);
      }
    } while (!sent);
    log.info("message sent {}", number);
  }

  private boolean doSend(long number) {
    val messageInput = By.id("conversation-input-bar-text");
    waitAndClick(messageInput);

    val msgElement = driver.findElement(messageInput);
    msgElement.sendKeys("message " + number);
    msgElement.sendKeys(Keys.ENTER);
    // TODO wait until message is displayed instead
    sleep(100L);
    return true;
  }

  private void sleep(long l) {
    try {
      Thread.sleep(l);
    } catch (InterruptedException ignored) {
    }
  }

  public static class CannotSendException extends RuntimeException {
    CannotSendException(String s) {
      super(s);
    }
  }
}
