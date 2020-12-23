/*
 * Copyright ©2015-2020 Jaemon. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dingerframework.listeners;

import com.dingerframework.core.DingerDefinitionResolver;
import com.dingerframework.core.entity.enums.DingerType;
import com.dingerframework.entity.enums.ExceptionEnum;
import com.dingerframework.exception.DingerException;
import com.dingerframework.utils.DingerUtils;
import com.dingerframework.core.DingerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;

import static com.dingerframework.constant.DingerConstant.SPOT_SEPERATOR;

/**
 * DingerXmlPreparedEvent
 *
 * @author Jaemon
 * @since 2.0
 */
public class DingerXmlPreparedEvent
        extends DingerDefinitionResolver
        implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {
    private static final Logger log = LoggerFactory.getLogger(DingerXmlPreparedEvent.class);

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        log.info("ready to execute dinger analysis.");

        registerDefaultDingerConfig(event.getEnvironment());

        try {
            ApplicationEventTimeTable.dingerClasses = doAnalysis(event);
        } catch (Exception ex) {
            throw new DingerException(ex, ExceptionEnum.UNKNOWN);
        }
    }

    /**
     * 注册默认的Dinger机器人信息, 即配置文件内容
     *
     * @param environment
     *          environment
     */
    private void registerDefaultDingerConfig(Environment environment) {
        for (DingerType dingerType : enabledDingerTypes) {
            String dingers = DINGER_PROPERTIES_PREFIX + "dingers" + SPOT_SEPERATOR + dingerType.name().toLowerCase() + SPOT_SEPERATOR;
            String tokenIdProp = dingers + "token-id";
            String secretProp = dingers + "secret";
            String decryptProp = dingers + "decrypt";
            String decryptKeyProp = dingers + "decryptKey";
            String asyncExecuteProp = dingers + "async";

            if (DingerUtils.isEmpty(tokenIdProp)) {
                if (log.isDebugEnabled()) {
                    log.debug("dinger={} is not open.", dingerType);
                }
                continue;
            }
            String tokenId = environment.getProperty(tokenIdProp);
            String secret = environment.getProperty(secretProp);
            boolean decrypt = getProperty(environment, decryptProp);
            boolean async = getProperty(environment, asyncExecuteProp);
            DingerConfig defaultDingerConfig = new DingerConfig(tokenId);
            defaultDingerConfig.setDingerType(dingerType);
            defaultDingerConfig.setSecret(secret);
            if (decrypt) {
                defaultDingerConfig.setDecryptKey(
                        environment.getProperty(decryptKeyProp)
                );
            }
            defaultDingerConfig.setAsyncExecute(async);

            defaultDingerConfig.check();
            defaultDingerConfigs.put(dingerType, defaultDingerConfig);
        }
    }

    /**
     * getProperty
     *
     * @param environment  environment
     * @param prop prop
     * @return prop value
     */
    private boolean getProperty(Environment environment, String prop) {
        if (environment.getProperty(prop) != null) {
            return environment.getProperty(prop, boolean.class);
        }
        return false;
    }
}