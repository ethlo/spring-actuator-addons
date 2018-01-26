package com.ethlo.spring.actuator;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.boot.actuate.endpoint.AbstractEndpoint;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * This end-point exports the configuration properties of the application
 * context and matches them with the property meta-data available to give a
 * powerful, documented view of the current system configuration.
 * 
 * Reference documentation this class uses to extract relevant data:
 * https://docs.spring.io/spring-boot/docs/current/reference/html/configuration-metadata.html
 */
public class ConfigurationPropertiesEndpoint extends AbstractEndpoint<Map<String, Object>>
{
    public static final String ADDITIONAL_PATH = "META-INF/additional-spring-configuration-metadata.json";
    public static final String PATH = "META-INF/spring-configuration-metadata.json";
    private static final String[] REGEX_PARTS = { "*", "$", "^", "+" };

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private boolean documentedOnly = false;
    private List<Pattern> includes = new ArrayList<>();
    private List<Pattern> excludes = new ArrayList<>();
    private List<Pattern> keysToSanitize = new ArrayList<>();

    public ConfigurationPropertiesEndpoint(String actuatorId)
    {
        super(actuatorId);
        this.setKeysToSanitize("password", "secret", "key", "token", ".*credentials.*", "vcap_services");
        this.setInclude(".*");
        this.setExclude("^java\\..*", "^\\..*", "^user\\..*", "^sun\\..*");
    }

    public ConfigurationPropertiesEndpoint setKeysToSanitize(String... keysToSanitize)
    {
        this.keysToSanitize = wrapExpressions(keysToSanitize);
        return this;
    }

    public ConfigurationPropertiesEndpoint setInclude(String... include)
    {
        this.includes = wrapExpressions(include);
        return this;
    }

    public ConfigurationPropertiesEndpoint setExclude(String... exclude)
    {
        this.excludes = wrapExpressions(exclude);
        return this;
    }
    
    public ConfigurationPropertiesEndpoint setDocumentedOnly(boolean b)
    {
        this.documentedOnly = b;
        return this;
    }

    @Override
    public boolean isEnabled()
    {
        return true;
    }

    @Override
    public boolean isSensitive()
    {
        return true;
    }

    private List<Pattern> wrapExpressions(String... expressions)
    {
        return Arrays.asList(expressions).stream().map(this::getPattern).collect(Collectors.toList());
    }
    
    @SuppressWarnings("rawtypes")
    private Map<String, Object> getSpringEnvironmentProperties()
    {
        final Map<String, Object> props = new TreeMap<>();
        final MutablePropertySources propSrcs = ((AbstractEnvironment) getEnvironment()).getPropertySources();
        StreamSupport.stream(propSrcs.spliterator(), false).filter(ps -> ps instanceof EnumerablePropertySource).map(ps -> ((EnumerablePropertySource) ps).getPropertyNames())
                        .flatMap(Arrays::<String> stream).forEach(propName -> {
                            if (isIncluded(propName) && !isExcluded(propName))
                            {
                                props.put(propName, sanitize(propName, getEnvironment().getProperty(propName)));
                            }
                        });
        return props;
    }

    private boolean isIncluded(String propName)
    {
        for (Pattern include : this.includes)
        {
            if (include.matcher(propName).matches())
            {
                return true;
            }
        }
        return false;
    }

    private boolean isExcluded(String propName)
    {
        for (Pattern exclude : this.excludes)
        {
            if (exclude.matcher(propName).matches())
            {
                return true;
            }
        }
        return false;
    }

    private List<InputStream> loadResources(final String name) throws IOException
    {
        final List<InputStream> list = new ArrayList<>();
        final Enumeration<URL> systemResources = ClassLoader.getSystemClassLoader().getResources(name);
        while (systemResources.hasMoreElements())
        {
            list.add(systemResources.nextElement().openStream());
        }
        return list;
    }

    @Override
    public Map<String, Object> invoke()
    {
        final Map<String, PropertyDto> retVal = new TreeMap<>();

        final Map<String, Object> props = getSpringEnvironmentProperties();
        for (Entry<String, Object> e : props.entrySet())
        {
            final PropertyDto p = new PropertyDto();
            p.setName(e.getKey());
            p.setValue(e.getValue());
            retVal.put(p.getName(), p);
        }

        try
        {
            processMetaResource(retVal, loadResources(PATH));
            processMetaResource(retVal, loadResources(ADDITIONAL_PATH));
        }
        catch (IOException exc)
        {
            throw new RuntimeException(exc.getMessage(), exc);
        }
        
        if (documentedOnly)
        {
            final Iterator<Entry<String, PropertyDto>> iter = retVal.entrySet().iterator();
            while (iter.hasNext())
            {
                if (! StringUtils.hasLength(iter.next().getValue().getDescription()))
                {
                    iter.remove();
                }
            }
        }
        
        return Collections.singletonMap("properties", retVal.values());
    }

    private void processMetaResource(final Map<String, PropertyDto> retVal, final List<InputStream> descs) throws IOException
    {
        for (InputStream d : descs)
        {
            final JsonNode rootNode = OBJECT_MAPPER.readTree(d);
            processProperties(retVal, rootNode);
            processValueHints(retVal, rootNode);
        }
    }

    private void processValueHints(final Map<String, PropertyDto> retVal, final JsonNode rootNode)
    {
        rootNode.path("hints").forEach(hintNode -> retVal.forEach((k, v) -> {
            final String propName = hintNode.path("name").textValue();
            if (k.equals(propName))
            {
                final List<PropertyDto> allowedValues = new ArrayList<>();
                ((ArrayNode) hintNode.path("values")).forEach(valueNode -> {
                    final String value = valueNode.path("value").textValue();
                    final String description = valueNode.path("description").textValue();
                    final PropertyDto e = new PropertyDto();
                    e.setName(value);
                    e.setDescription(description);
                    allowedValues.add(e);
                });
                v.setAllowedValues(allowedValues);
            }
        }));
    }

    private void processProperties(final Map<String, PropertyDto> retVal, final JsonNode rootNode)
    {
        rootNode.path("properties").forEach(propDesc -> {
            final String propName = propDesc.path("name").textValue();
            retVal.forEach((k, v) -> {
                if (k.equals(propName))
                {
                    final String description = propDesc.path("description").textValue();
                    v.setDescription(description);

                    final String type = propDesc.path("type").textValue();
                    v.setType(type);

                    final String defaultValue = propDesc.path("defaultValue").textValue();
                    v.setDefaultValue(defaultValue);
                }
            });
        });
    }

    private Pattern getPattern(String value)
    {
        if (isRegex(value))
        {
            return Pattern.compile(value, Pattern.CASE_INSENSITIVE);
        }
        return Pattern.compile(".*" + value + "$", Pattern.CASE_INSENSITIVE);
    }

    private boolean isRegex(String value)
    {
        for (String part : REGEX_PARTS)
        {
            if (value.contains(part))
            {
                return true;
            }
        }
        return false;
    }

    public Object sanitize(String key, Object value)
    {
        for (Pattern pattern : this.keysToSanitize)
        {
            if (pattern.matcher(key).matches())
            {
                return (value == null ? null : "******");
            }
        }
        return value;
    }

    public class PropertyDto
    {
        private String name;
        private Object value;
        private String type;
        private String description;
        private String defaultValue;
        private List<PropertyDto> allowedValues;

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public Object getValue()
        {
            return value;
        }

        public void setValue(Object value)
        {
            this.value = value;
        }

        public String getType()
        {
            return type;
        }

        public void setType(String type)
        {
            this.type = type;
        }

        public String getDescription()
        {
            return description;
        }

        public void setDescription(String description)
        {
            this.description = description;
        }

        public String getDefaultValue()
        {
            return defaultValue;
        }

        public void setDefaultValue(String defaultValue)
        {
            this.defaultValue = defaultValue;
        }

        public List<PropertyDto> getAllowedValues()
        {
            return allowedValues;
        }

        public void setAllowedValues(List<PropertyDto> allowedValues)
        {
            this.allowedValues = allowedValues;
        }
    }
}
