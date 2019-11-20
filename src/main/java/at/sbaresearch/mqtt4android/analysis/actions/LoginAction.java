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

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LoginAction {

  WebDriver driver;
  ActionHelper helper;
  String hostname;
  String user;
  String password;

  public LoginAction(WebDriver driver, ActionHelper helper,
      @Value("${web.hostname}") String hostname,
      @Value("${login.user}") String user,
      @Value("${login.password}") String password) {
    this.driver = driver;
    this.helper = helper;
    this.hostname = hostname;
    this.user = user;
    this.password = password;
  }

  public void loadPage() {
    log.info("loadPage");
    driver.get(hostname + "/auth/?reason=expired#login");
  }

  public void login() {
    log.info("logging in...");

    By email = By.name("email");
    helper.blockingWaitForElement(email);
    log.info("login form found");

    driver.findElement(email).click();
    driver.findElement(email).sendKeys(user);
    driver.findElement(By.name("password-login")).sendKeys(password);
    driver.findElement(By.name("password-login")).sendKeys(Keys.ENTER);

    try {
      Thread.sleep(200L);
    } catch (InterruptedException ignored) {
    }
    // confirm new device if needed
    try {
      helper.blockingWaitForElement(By.tagName("button"));
      driver.findElement(By.tagName("button")).click();
      log.info("new device confirmed");
    } catch (ElementNotInteractableException ignored) {
      log.info("device already known");
    }

    helper.blockingWaitForElement(By.cssSelector(".conversation-list-cell-name"));
    log.info("login success");
  }

}
