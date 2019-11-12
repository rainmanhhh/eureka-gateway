package com.github.rainmanhhh.gateway.filter;

import org.springframework.core.Ordered;
import org.springframework.web.server.WebFilter;

public interface IGatewayFilter extends WebFilter, Ordered {
    boolean isEnabled();

    void setEnabled(boolean enabled);

    void setOrder(int order);
}
