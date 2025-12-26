package com.checkstyleplus;

import com.puppycrawl.tools.checkstyle.api.Configuration;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;

import java.util.*;

public class FilteredConfiguration implements Configuration {

    private final Configuration delegate;
    private final Set<String> disabledModules;

    public FilteredConfiguration(Configuration delegate, Set<String> disabledModules) {
        this.delegate = delegate;
        this.disabledModules = disabledModules;
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public Configuration[] getChildren() {
        List<Configuration> filtered = new ArrayList<>();
        for (Configuration c : delegate.getChildren()) {
            if (!disabledModules.contains(c.getName())) {
                filtered.add(new FilteredConfiguration(c, disabledModules));
            }
        }
        return filtered.toArray(new Configuration[0]);
    }

    /**
     * @deprecated Deprecated in Checkstyle since 8.45.
     * Use {@link #getProperty(String)} instead.
     */
    @Deprecated
    @Override
    public String getAttribute(String name) throws CheckstyleException {
        return delegate.getAttribute(name);
    }

    /**
     * @deprecated Deprecated in Checkstyle since 8.45.
     * Use {@link #getPropertyNames()} instead.
     */
    @Deprecated
    @Override
    public String[] getAttributeNames() {
        return delegate.getAttributeNames();
    }

    @Override
    public Map<String, String> getMessages() {
        return delegate.getMessages();
    }

    @Override
    public String getProperty(String key) throws CheckstyleException {
        return delegate.getProperty(key);
    }

    @Override
    public String[] getPropertyNames() {
        return delegate.getPropertyNames();
    }
}
