package org.jboss.weld.context.http;

import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.context.AbstractConversationContext;
import org.jboss.weld.context.beanstore.BoundBeanStore;
import org.jboss.weld.context.beanstore.NamingScheme;
import org.jboss.weld.context.beanstore.http.EagerSessionBeanStore;
import org.jboss.weld.context.beanstore.http.LazySessionBeanStore;
import org.jboss.weld.servlet.SessionHolder;
import org.jboss.weld.util.collections.EnumerationIterator;

public class HttpConversationContextImpl extends AbstractConversationContext<HttpServletRequest, HttpSession> implements HttpConversationContext {

    // There is no need to store FQCN in a session key
    private static final String NAMING_SCHEME_PREFIX = "WELD_C";

    public HttpConversationContextImpl(String contextId, ServiceRegistry services) {
        super(contextId, services);
    }

    @Override
    protected void setSessionAttribute(HttpServletRequest request, String name, Object value, boolean create) {
        if (create || SessionHolder.getSessionIfExists() != null) {
            getSessionFromRequest(request, true).setAttribute(name, value);
        }
    }

    @Override
    protected Object getSessionAttribute(HttpServletRequest request, String name, boolean create) {
        if (create || SessionHolder.getSessionIfExists() != null) {
            return getSessionFromRequest(request, true).getAttribute(name);
        } else {
            return null;
        }
    }

    @Override
    protected void removeRequestAttribute(HttpServletRequest request, String name) {
        request.removeAttribute(name);
    }

    @Override
    protected void setRequestAttribute(HttpServletRequest request, String name, Object value) {
        request.setAttribute(name, value);
    }

    @Override
    protected Object getRequestAttribute(HttpServletRequest request, String name) {
        return request.getAttribute(name);
    }

    @Override
    protected BoundBeanStore createRequestBeanStore(NamingScheme namingScheme, HttpServletRequest request) {
        return new LazySessionBeanStore(request, namingScheme, false);
    }

    @Override
    protected BoundBeanStore createSessionBeanStore(NamingScheme namingScheme, HttpSession session) {
        return new EagerSessionBeanStore(namingScheme, session);
    }

    @Override
    protected Object getSessionAttributeFromSession(HttpSession session, String name) {
        return session.getAttribute(name);
    }

    @Override
    protected HttpSession getSessionFromRequest(HttpServletRequest request, boolean create) {
        return SessionHolder.getSession(request, create);
    }

    @Override
    protected String getNamingSchemePrefix() {
        return NAMING_SCHEME_PREFIX;
    }

    @Override
    protected Iterator<String> getSessionAttributeNames(HttpSession session) {
        return new EnumerationIterator<String>(session.getAttributeNames());
    }

}
