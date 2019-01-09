/*
 * Copyright (C) 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.alicloud.sms;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cloud.alicloud.context.sms.SmsConfigProperties;
import org.springframework.cloud.alicloud.sms.base.MessageListener;

import com.aliyuncs.profile.DefaultProfile;

/**
 * @author pbting
 */
public class SmsInitializer implements BeanPostProcessor, SmartInitializingSingleton {

	private final static Log log = LogFactory.getLog(SmsInitializer.class);

	private SmsConfigProperties msConfigProperties;
	private ISmsService smsService;

	public SmsInitializer(SmsConfigProperties msConfigProperties,
			ISmsService smsService) {
		this.msConfigProperties = msConfigProperties;
		this.smsService = smsService;
	}

	@Override
	public void afterSingletonsInstantiated() {
		// 整个application context refreshed then do
		// 可自助调整超时时间
		System.setProperty("sun.net.client.defaultConnectTimeout",
				msConfigProperties.getConnnectTimeout());
		System.setProperty("sun.net.client.defaultReadTimeout",
				msConfigProperties.getReadTimeout());
		// 初始化acsClient,暂不支持region化
		try {
			DefaultProfile.addEndpoint("cn-hangzhou", "cn-hangzhou",
					SmsConfigProperties.smsProduct, SmsConfigProperties.smsDomain);
		}
		catch (Exception e) {
			log.error("initializer the sms cause an exception", e);
		}
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName)
			throws BeansException {
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName)
			throws BeansException {
		if (bean instanceof MessageListener) {
			initMessageListener((MessageListener) bean);
		}
		return bean;
	}

	private void initMessageListener(MessageListener messageListener) {
		if (SmsReportMessageListener.class.isInstance(messageListener)) {
			if (msConfigProperties.getReportQueueName() != null
					&& msConfigProperties.getReportQueueName().trim().length() > 0) {
				smsService.startSmsReportMessageListener(
						(SmsReportMessageListener) messageListener);
				return;
			}

			throw new IllegalArgumentException("the SmsReport queue name for "
					+ messageListener.getClass().getCanonicalName() + " must be set.");
		}

		if (SmsUpMessageListener.class.isInstance(messageListener)) {

			if (msConfigProperties.getUpQueueName() != null
					&& msConfigProperties.getUpQueueName().trim().length() > 0) {
				smsService.startSmsUpMessageListener(
						(SmsUpMessageListener) messageListener);
				return;
			}

			throw new IllegalArgumentException("the SmsUp queue name for "
					+ messageListener.getClass().getCanonicalName() + " must be set.");
		}
	}
}