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
package de.schnippsche.solarreader.test;

import de.schnippsche.solarreader.backend.connection.general.ConnectionFactory;
import de.schnippsche.solarreader.backend.connection.network.HttpConnection;
import de.schnippsche.solarreader.backend.util.Setting;
import de.schnippsche.solarreader.database.ProviderData;
import de.schnippsche.solarreader.plugins.solarprognose.Solarprognose;
import org.junit.jupiter.api.Test;

class SolarprognoseTest {
  @Test
  void test() throws Exception {
    GeneralTestHelper generalTestHelper = new GeneralTestHelper();
    ConnectionFactory<HttpConnection> testFactory =
        knownConfiguration -> new SolarprognoseHttpConnection();

    Solarprognose provider = new Solarprognose(testFactory);
    ProviderData providerData = new ProviderData();
    providerData.setPluginName("Solarprognose");
    providerData.setName("Solarprognose Test");
    providerData.setSetting(provider.getDefaultProviderSetting());
    Setting setting = new Setting();
    setting.setConfigurationValue("token", "12345");
    setting.setConfigurationValue("algorithm", "mosmix");
    setting.setConfigurationValue("item", "item");
    setting.setConfigurationValue("elementid", "0815");
    setting.setProviderHost("www.solarprognose.de");
    providerData.setSetting(setting);
    provider.setProviderData(providerData);
    generalTestHelper.testProviderInterface(provider);
  }
}
