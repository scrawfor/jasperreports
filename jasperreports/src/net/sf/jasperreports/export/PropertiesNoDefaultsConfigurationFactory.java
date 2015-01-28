/*
 * JasperReports - Free Java Reporting Library.
 * Copyright (C) 2001 - 2014 TIBCO Software Inc. All rights reserved.
 * http://www.jaspersoft.com
 *
 * Unless you have purchased a commercial license agreement from Jaspersoft,
 * the following license terms apply:
 *
 * This program is part of JasperReports.
 *
 * JasperReports is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JasperReports is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JasperReports. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.jasperreports.export;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import net.sf.jasperreports.engine.JRPropertiesHolder;
import net.sf.jasperreports.engine.JRPropertiesMap;
import net.sf.jasperreports.engine.JRPropertiesUtil;
import net.sf.jasperreports.engine.JRPropertiesUtil.PropertySuffix;
import net.sf.jasperreports.engine.JRRuntimeException;
import net.sf.jasperreports.engine.JasperReportsContext;
import net.sf.jasperreports.engine.type.NamedEnum;
import net.sf.jasperreports.export.annotations.ExporterProperty;

import org.apache.commons.lang.ClassUtils;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class PropertiesNoDefaultsConfigurationFactory<C extends CommonExportConfiguration>
{
	/**
	 * 
	 */
	private final JasperReportsContext jasperReportsContext;
	
	/**
	 * 
	 */
	public PropertiesNoDefaultsConfigurationFactory(JasperReportsContext jasperReportsContext)
	{
		this.jasperReportsContext = jasperReportsContext;
	}

	
	/**
	 * 
	 */
	public C getConfiguration(final Class<C> configurationInterface, final JRPropertiesHolder propertiesHolder)
	{
		return getProxy(configurationInterface, new PropertiesInvocationHandler(propertiesHolder));
	}


	/**
	 * 
	 */
	private final C getProxy(Class<?> clazz, InvocationHandler handler)
	{
		List<Class<?>> allInterfaces = new ArrayList<Class<?>>();

		if (clazz.isInterface())
		{
			allInterfaces.add(clazz);
		}
		else
		{
			@SuppressWarnings("unchecked")
			List<Class<?>> lcInterfaces = ClassUtils.getAllInterfaces(clazz);
			allInterfaces.addAll(lcInterfaces);
		}

		@SuppressWarnings("unchecked")
		C proxy =
			(C)Proxy.newProxyInstance(
				ExporterConfiguration.class.getClassLoader(),
				allInterfaces.toArray(new Class<?>[allInterfaces.size()]),
				handler
				);
		
		return proxy;
	}


	/**
	 * 
	 */
	class PropertiesInvocationHandler implements InvocationHandler
	{
		private final JRPropertiesHolder propertiesHolder;
		
		/**
		 * 
		 */
		public PropertiesInvocationHandler(final JRPropertiesHolder propertiesHolder)
		{
			this.propertiesHolder = propertiesHolder;
		}
		
		/**
		 * 
		 */
		public Object invoke(
			Object proxy, 
			Method method, 
			Object[] args
			) throws Throwable 
		{
			return getPropertyValue(method, propertiesHolder);
		}
	}
	
	
	/**
	 * 
	 */
	protected Object getPropertyValue(Method method, JRPropertiesHolder propertiesHolder)
	{
		Object value = null;
		ExporterProperty exporterProperty = method.getAnnotation(ExporterProperty.class);
		if (exporterProperty != null)
		{
			value = getPropertyValue(jasperReportsContext, propertiesHolder, exporterProperty, method.getReturnType());
		}
		return value;
	}
	
	
	/**
	 * 
	 */
	public static Object getPropertyValue(
		JasperReportsContext jasperReportsContext,
		JRPropertiesHolder propertiesHolder,
		ExporterProperty exporterProperty, 
		Class<?> type 
		)
	{
		Object value = null;
		
		String propertyName = exporterProperty.value();
		
		if (String[].class.equals(type))
		{
			List<PropertySuffix> properties = JRPropertiesUtil.getProperties(propertiesHolder, propertyName);
			if (properties != null && !properties.isEmpty())
			{
				String[] values = new String[properties.size()];
				for(int i = 0; i < values.length; i++)
				{
					values[i] = properties.get(i).getValue();
				}
				
				value = values;
			}
		}
		else
		{
			String strValue = null;

			JRPropertiesMap propertiesMap = propertiesHolder.getPropertiesMap();
			if (propertiesMap != null && propertiesMap.containsProperty(propertyName))
			{
				strValue = propertiesMap.getProperty(propertyName);
			}

			if (strValue != null)
			{
				if (String.class.equals(type))
				{
					value = strValue;
				}
				else if (Character.class.equals(type))
				{
					value = JRPropertiesUtil.asCharacter(strValue);
				}
				else if (Integer.class.equals(type))
				{
					value = JRPropertiesUtil.asInteger(strValue);
				}
				else if (Long.class.equals(type))
				{
					value = JRPropertiesUtil.asLong(strValue);
				}
				else if (Float.class.equals(type))
				{
					value = JRPropertiesUtil.asFloat(strValue);
				}
				else if (Boolean.class.equals(type))
				{
					value = JRPropertiesUtil.asBoolean(strValue);
				}
				else if (NamedEnum.class.isAssignableFrom(type))
				{
					try
					{
						Method byNameMethod = type.getMethod("getByName", new Class<?>[]{String.class});
						value = byNameMethod.invoke(null, strValue);
					}
					catch (NoSuchMethodException e)
					{
						throw new JRRuntimeException(e);
					}
					catch (InvocationTargetException e)
					{
						throw new JRRuntimeException(e);
					}
					catch (IllegalAccessException e)
					{
						throw new JRRuntimeException(e);
					}
				}
				else
				{
					throw new JRRuntimeException("Export property type " + type + " not supported.");
				}
			}
		}
		
		return value;
	}
}
