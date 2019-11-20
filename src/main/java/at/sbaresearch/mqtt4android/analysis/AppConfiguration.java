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

import at.sbaresearch.mqtt4android.analysis.actions.ConverstationActions;
import at.sbaresearch.mqtt4android.analysis.actions.LoginAction;
import at.sbaresearch.mqtt4android.analysis.schedule.RandomMessageSender;
import at.sbaresearch.mqtt4android.analysis.schedule.SleepManager;
import at.sbaresearch.mqtt4android.analysis.statistics.Statistics;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class AppConfiguration {

  /**
   * this needs chrome web driver; e.g. on debian: apt install chromium-driver
   */
  @Bean(destroyMethod = "quit")
  public WebDriver webDriver(@Value("${chrome.path}") String chromePath,
      StateManager stateManager) {
    val opts = new ChromeOptions();
    opts.setBinary(chromePath);

    opts.setHeadless(true);
    opts.addArguments(
        "--disable-notifications",
        "--suppress-message-center-popups",
        "--user-agent=Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.97 Safari/537.36",
        // wire-webapp will not render with UA "HeadlessChrome"
        "--user-data-dir=" + stateManager.getProfilePath()
    );

    ChromeDriver driver = new ChromeDriver(opts);
    driver.manage().window().setSize(new Dimension(1662, 1027));

    return driver;
  }

  @Bean
  public AnalysisRunner analysisRunner(
      LoginAction loginAction, ConverstationActions converstationActions,
      RandomMessageSender randomMessageSender,
      LogoutWatchdog logoutWatchdog, DeliveredWatchdog deliveredWatchdog,
      Statistics statistics,
      @Value("${analysis.seed}") long seed, @Value("${analysis.timelimit}") int timelimitMins) {
    return new AnalysisRunner(loginAction, converstationActions, randomMessageSender,
        logoutWatchdog, deliveredWatchdog, statistics, seed, timelimitMins);
  }

  @Bean
  public RandomMessageSender randomMessageSender(
      ConverstationActions converstationActions,
      SleepManager sleepManager, Statistics statistics,
      @Value("${message-sender.sleep.min}") int min,
      @Value("${message-sender.sleep.max}") int max) {
    return new RandomMessageSender(converstationActions, sleepManager, statistics, min, max);
  }

  @Bean
  public WebDriverWait webDriverWait(WebDriver driver) {
    return new WebDriverWait(driver, 10);
  }

  @Bean
  public StateManager stateManager(@Value("${chrome.profile.dir}") String profileDir) {
    return new StateManager(profileDir);
  }

}
