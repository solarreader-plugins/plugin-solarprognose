/*
 * Copyright (c) 2024-2025 Stefan Toengi
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package de.schnippsche.solarreader.plugins.solarprognose;

import de.schnippsche.solarreader.backend.calculator.MapCalculator;
import de.schnippsche.solarreader.backend.connection.general.ConnectionFactory;
import de.schnippsche.solarreader.backend.connection.network.HttpConnection;
import de.schnippsche.solarreader.backend.connection.network.HttpConnectionFactory;
import de.schnippsche.solarreader.backend.field.FieldType;
import de.schnippsche.solarreader.backend.field.PropertyField;
import de.schnippsche.solarreader.backend.protocol.KnownProtocol;
import de.schnippsche.solarreader.backend.provider.AbstractHttpProvider;
import de.schnippsche.solarreader.backend.provider.CommandProviderProperty;
import de.schnippsche.solarreader.backend.provider.ProviderProperty;
import de.schnippsche.solarreader.backend.provider.SupportedInterface;
import de.schnippsche.solarreader.backend.singleton.GlobalUsrStore;
import de.schnippsche.solarreader.backend.table.Table;
import de.schnippsche.solarreader.backend.table.TableCell;
import de.schnippsche.solarreader.backend.table.TableColumn;
import de.schnippsche.solarreader.backend.table.TableColumnType;
import de.schnippsche.solarreader.backend.table.TableRow;
import de.schnippsche.solarreader.backend.util.JsonTools;
import de.schnippsche.solarreader.backend.util.Setting;
import de.schnippsche.solarreader.backend.util.StringConverter;
import de.schnippsche.solarreader.database.Activity;
import de.schnippsche.solarreader.frontend.ui.HtmlInputType;
import de.schnippsche.solarreader.frontend.ui.HtmlWidth;
import de.schnippsche.solarreader.frontend.ui.UIInputElementBuilder;
import de.schnippsche.solarreader.frontend.ui.UIList;
import de.schnippsche.solarreader.frontend.ui.UISelecttElementBuilder;
import de.schnippsche.solarreader.frontend.ui.UITextElementBuilder;
import de.schnippsche.solarreader.frontend.ui.ValueText;
import de.schnippsche.solarreader.plugin.PluginMetadata;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;
import org.tinylog.Logger;

/**
 * The {@link Solarprognose} class is a provider class responsible for interacting with the Solar
 * Prognosis API. It extends {@link AbstractHttpProvider} and implements methods to fetch solar
 * power prediction data from a web service. This includes obtaining solar forecasts,
 * weather-related information, and other relevant data to help in forecasting solar energy
 * production.
 *
 * <p>This class is used to send HTTP requests to the Solar Prognosis API, process the responses,
 * and retrieve data that can be used to analyze and predict solar energy production based on
 * weather patterns and other influencing factors.
 *
 * <p>Solar power data is typically used in applications related to energy management, solar panel
 * efficiency optimization, and grid management.
 */
@PluginMetadata(
    name = "Solarprognose",
    version = "1.0.1",
    author = "Stefan Töngi",
    url = "https://github.com/solarreader-plugins/plugin-Solarprognose",
    svgImage = "solarprognose.svg",
    supportedInterfaces = {SupportedInterface.URL},
    usedProtocol = KnownProtocol.HTTP,
    supports = "Solarprognose V1")
public class Solarprognose extends AbstractHttpProvider {
  private static final String BASE_URL =
      "http://{provider_host}/web/solarprediction/api/v1?access-token={token}&item={item}&id={elementid}&type=hourly&_format=json&algorithm={algorithm}&project=solarreader&start_epoch_time={starttime}&end_epoch_time={endtime}";
  private static final String DATA = "data_";
  private static final String TOKEN = "token";
  private static final String ELEMENTID = "elementid";
  private static final String ALGORITHM = "algorithm";
  private static final String ITEM = "item";

  /**
   * Constructs a new instance of the {@link Solarprognose} class using the default HTTP connection
   * factory. This constructor provides a simple way to initialize the {@link Solarprognose} object
   * without needing to manually provide a {@link ConnectionFactory}. It uses the default connection
   * factory configuration. The constructor initializes the connection to the Solar Prognosis API
   * and sets up the HTTP connection handling.
   */
  public Solarprognose() {
    this(new HttpConnectionFactory());
  }

  /**
   * Constructs a new instance of the {@link Solarprognose} class with a custom {@link
   * ConnectionFactory} for managing HTTP connections. This constructor allows for more control over
   * how HTTP connections are created, which is useful when specific configurations or behaviors are
   * needed for HTTP requests (e.g., custom timeout settings, connection pooling).
   *
   * @param connectionFactory the {@link ConnectionFactory} to use for creating HTTP connections
   */
  public Solarprognose(ConnectionFactory<HttpConnection> connectionFactory) {
    super(connectionFactory);
    Logger.debug("instantiate {}", this.getClass().getName());
  }

  /**
   * Retrieves the resource bundle for the plugin based on the specified locale.
   *
   * <p>This method overrides the default implementation to return a {@link ResourceBundle} for the
   * plugin using the provided locale.
   *
   * @return The {@link ResourceBundle} for the plugin, localized according to the specified locale.
   */
  @Override
  public ResourceBundle getPluginResourceBundle() {
    return ResourceBundle.getBundle("solarprognose", locale);
  }

  @Override
  public Activity getDefaultActivity() {
    return new Activity(LocalTime.of(2, 0, 0), LocalTime.of(21, 0, 0), 1, TimeUnit.HOURS);
  }

  @Override
  public Optional<UIList> getProviderDialog() {
    UIList uiList = new UIList();
    uiList.addElement(
        new UITextElementBuilder()
            .withLabel(resourceBundle.getString("solarprognose.title.text"))
            .build());
    uiList.addElement(
        new UIInputElementBuilder()
            .withId("id-solarprognose-token")
            .withRequired(true)
            .withType(HtmlInputType.TEXT)
            .withColumnWidth(HtmlWidth.HALF)
            .withLabel(resourceBundle.getString("solarprognose.token.text"))
            .withName(TOKEN)
            .withPlaceholder(resourceBundle.getString("solarprognose.token.text"))
            .withTooltip(resourceBundle.getString("solarprognose.token.tooltip"))
            .withInvalidFeedback(resourceBundle.getString("solarprognose.token.error"))
            .build());
    uiList.addElement(
        new UIInputElementBuilder()
            .withId("id-solarprognose-elementid")
            .withRequired(true)
            .withType(HtmlInputType.NUMBER)
            .withStep("any")
            .withColumnWidth(HtmlWidth.HALF)
            .withLabel(resourceBundle.getString("solarprognose.elementid.text"))
            .withName(ELEMENTID)
            .withPlaceholder(resourceBundle.getString("solarprognose.elementid.text"))
            .withTooltip(resourceBundle.getString("solarprognose.elementid.tooltip"))
            .withInvalidFeedback(resourceBundle.getString("solarprognose.elementid.error"))
            .build());
    List<ValueText> options = new ArrayList<>();
    options.add(new ValueText(""));
    options.add(new ValueText("own-v1"));
    options.add(new ValueText("mosmix"));
    uiList.addElement(
        new UISelecttElementBuilder()
            .withColumnWidth(HtmlWidth.HALF)
            .withName(ALGORITHM)
            .withLabel(resourceBundle.getString("solarprognose.algorithm.text"))
            .withTooltip(resourceBundle.getString("solarprognose.algorithm.tooltip"))
            .withOptions(options)
            .build());
    uiList.addElement(
        new UIInputElementBuilder()
            .withId("id-solarprognose-item")
            .withRequired(true)
            .withType(HtmlInputType.TEXT)
            .withColumnWidth(HtmlWidth.HALF)
            .withLabel(resourceBundle.getString("solarprognose.item.text"))
            .withName(ITEM)
            .withPlaceholder(resourceBundle.getString("solarprognose.item.text"))
            .withTooltip(resourceBundle.getString("solarprognose.item.tooltip"))
            .withInvalidFeedback(resourceBundle.getString("solarprognose.item.error"))
            .build());
    return Optional.of(uiList);
  }

  @Override
  public Optional<List<ProviderProperty>> getSupportedProperties() {
    List<ProviderProperty> providerProperties = new ArrayList<>();
    CommandProviderProperty actionProperty = new CommandProviderProperty();
    actionProperty.setName("Solarprognose");
    actionProperty.setCommand(BASE_URL);
    List<PropertyField> propertyFields = new ArrayList<>();
    providerProperties.add(actionProperty);
    PropertyField propertyField;
    for (int hour = 0; hour < 24; hour++) {
      propertyField = new PropertyField("prognose_" + hour, FieldType.NUMBER);
      propertyFields.add(propertyField);
      propertyField.setUnit("kw");
      propertyField.setIndex(DATA + hour + "_0");
      propertyField.setNote("prognose fur current date at hour " + hour);
      propertyField = new PropertyField("prognose_accumulated_" + hour, FieldType.NUMBER);
      propertyFields.add(propertyField);
      propertyField.setUnit("kwh");
      propertyField.setIndex(DATA + hour + "_1");
      propertyField.setNote("accumulated prognose fur current date from hour 0 to hour " + hour);
      propertyField = new PropertyField("timestamp_" + hour, FieldType.TIMESTAMP);
      propertyFields.add(propertyField);
      propertyField.setUnit("seconds");
      propertyField.setIndex(DATA + hour + "_ts");
      propertyField.setNote("timestamp for hour " + hour);
    }
    actionProperty.getPropertyFieldList().addAll(propertyFields);
    return Optional.of(providerProperties);
  }

  @Override
  public Optional<List<Table>> getDefaultTables() {
    List<Table> tables = new ArrayList<>(1);
    Table table = new Table("Wetterprognose");
    TableColumn dateColumn = new TableColumn("Datum", TableColumnType.STRING);
    dateColumn.checkPrimaryKey();
    table.addColumn(dateColumn);
    TableColumn prognoseColumn = new TableColumn("Prognose_W", TableColumnType.NUMBER);
    prognoseColumn.checkPrimaryKey();
    table.addColumn(prognoseColumn);
    TableColumn prognoseTotalColumn = new TableColumn("Prognose_Wh", TableColumnType.NUMBER);
    prognoseTotalColumn.checkPrimaryKey();
    table.addColumn(prognoseTotalColumn);
    TableColumn timestampColumn = new TableColumn("timestamp", TableColumnType.TIMESTAMP);
    timestampColumn.checkPrimaryKey();
    table.addColumn(timestampColumn);
    for (int hour = 0; hour < 24; hour++) {
      TableRow tableRow = new TableRow();
      String preCondition = String.format("timestamp_%d != null", hour);
      tableRow.addCell(
          new TableCell(
              preCondition, String.format("DT_DATE_FORMAT(timestamp_%d, \"dd.MM.yyyy\")", hour)));
      tableRow.addCell(new TableCell(preCondition, String.format("prognose_%d", hour)));
      tableRow.addCell(new TableCell(preCondition, String.format("prognose_accumulated_%d", hour)));
      tableRow.addCell(new TableCell(preCondition, String.format("timestamp_%d", hour)));
      table.addTableRow(tableRow);
    }
    tables.add(table);
    return Optional.of(tables);
  }

  @Override
  public Setting getDefaultProviderSetting() {
    Setting setting = new Setting();
    setting.setConfigurationValue(ALGORITHM, "mosmix");
    setting.setConfigurationValue(ITEM, "plant");
    setting.setProviderHost("www.solarprognose.de");
    setting.setReadTimeoutMilliseconds(5000);
    return setting;
  }

  @Override
  public String testProviderConnection(Setting testSetting)
      throws IOException, InterruptedException {
    HttpConnection connection = connectionFactory.createConnection(testSetting);
    URL testUrl = getApiUrl(testSetting, BASE_URL);
    connection.test(testUrl, HttpConnection.CONTENT_TYPE_JSON);
    return resourceBundle.getString("solarprognose.connection.successful");
  }

  @Override
  public void doOnFirstRun() {
    doStandardFirstRun();
  }

  @Override
  public boolean doActivityWork(Map<String, Object> variables)
      throws IOException, InterruptedException {
    workProperties(getConnection(), variables);
    return true;
  }

  private boolean handleMap(Map<String, Object> readValues) {
    BigDecimal status = (BigDecimal) readValues.getOrDefault("status", "0");
    if (status.intValue() < 0) {
      String message = (String) readValues.getOrDefault("message", "");
      Logger.error("solarprognose returns error code {}, message:{}", status, message);
      return false;
    }
    Map<String, Object> modifiedMap = new HashMap<>(readValues);
    for (Map.Entry<String, Object> entry : readValues.entrySet()) {
      if (entry.getKey().startsWith(DATA) && entry.getValue() != null) {
        String[] split = entry.getKey().split("_");
        if (split.length == 3) {
          long unixTimestamp = new StringConverter(split[1]).toLong(); // ts in unix seconds
          Instant instant = Instant.ofEpochSecond(unixTimestamp);
          LocalDateTime localDateTime = instant.atZone(ZoneOffset.UTC).toLocalDateTime();
          String newKey = DATA + localDateTime.getHour() + "_";
          modifiedMap.put(newKey + split[2], entry.getValue());
          modifiedMap.put(newKey + "ts", unixTimestamp);
        }
      }
    }

    readValues.putAll(modifiedMap);

    return true;
  }

  private URL getApiUrl(Setting setting, String urlPattern) throws IOException {
    Map<String, String> configurationValues = new HashMap<>();
    configurationValues.put(Setting.PROVIDER_HOST, setting.getProviderHost());
    configurationValues.put(Setting.PROVIDER_PORT, "" + setting.getProviderPort());
    Instant startInstant = GlobalUsrStore.getInstance().getCurrentZonedDateTime().toInstant();
    Instant endInstant = startInstant.plus(23, ChronoUnit.HOURS);
    long start = startInstant.getEpochSecond();
    long end = endInstant.getEpochSecond();
    configurationValues.put("starttime", String.valueOf(start));
    configurationValues.put("endtime", String.valueOf(end));
    configurationValues.put(TOKEN, setting.getConfigurationValueAsString(TOKEN));
    configurationValues.put(ITEM, setting.getConfigurationValueAsString(ITEM));
    configurationValues.put(ELEMENTID, setting.getConfigurationValueAsString(ELEMENTID));
    configurationValues.put(ALGORITHM, setting.getConfigurationValueAsString(ALGORITHM));
    String urlString =
        new StringConverter(urlPattern).replaceNamedPlaceholders(configurationValues);
    Logger.debug("url:{}", urlString);
    return new StringConverter(urlString).toUrl();
  }

  @Override
  protected void handleCommandProperty(
      HttpConnection httpConnection,
      CommandProviderProperty commandProviderProperty,
      Map<String, Object> variables)
      throws IOException, InterruptedException {
    String pattern = commandProviderProperty.getCommand();
    URL url = getApiUrl(providerData.getSetting(), pattern);
    Map<String, Object> readValues =
        new JsonTools().getSimpleMapFromJsonString(httpConnection.getAsString(url));
    if (handleMap(readValues)) {
      new MapCalculator()
          .calculate(readValues, commandProviderProperty.getPropertyFieldList(), variables);
    }
  }
}
