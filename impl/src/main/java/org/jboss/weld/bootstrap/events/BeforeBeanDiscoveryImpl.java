/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.weld.bootstrap.events;

import java.lang.annotation.Annotation;
import java.util.Collection;

import javax.enterprise.context.spi.Context;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.builder.AnnotatedTypeConfigurator;

import org.jboss.weld.bootstrap.BeanDeploymentArchiveMapping;
import org.jboss.weld.bootstrap.ContextHolder;
import org.jboss.weld.bootstrap.events.builder.AnnotatedTypeConfiguratorImpl;
import org.jboss.weld.bootstrap.spi.Deployment;
import org.jboss.weld.literal.InterceptorBindingTypeLiteral;
import org.jboss.weld.literal.NormalScopeLiteral;
import org.jboss.weld.literal.QualifierLiteral;
import org.jboss.weld.literal.ScopeLiteral;
import org.jboss.weld.literal.StereotypeLiteral;
import org.jboss.weld.logging.BootstrapLogger;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.metadata.TypeStore;
import org.jboss.weld.metadata.cache.MetaAnnotationStore;
import org.jboss.weld.resources.ClassTransformer;
import org.jboss.weld.resources.ReflectionCache;
import org.jboss.weld.util.annotated.AnnotatedTypeWrapper;

public class BeforeBeanDiscoveryImpl extends AbstractAnnotatedTypeRegisteringEvent implements BeforeBeanDiscovery {

    public static void fire(BeanManagerImpl beanManager, Deployment deployment, BeanDeploymentArchiveMapping bdaMapping, Collection<ContextHolder<? extends Context>> contexts) {
        BeforeBeanDiscoveryImpl event = new BeforeBeanDiscoveryImpl(beanManager, deployment, bdaMapping, contexts);
        event.finish();
        event.fire();
    }

    protected BeforeBeanDiscoveryImpl(BeanManagerImpl beanManager, Deployment deployment, BeanDeploymentArchiveMapping bdaMapping, Collection<ContextHolder<? extends Context>> contexts) {
        super(beanManager, BeforeBeanDiscovery.class, bdaMapping, deployment, contexts);
    }

    @Override
    public void addQualifier(Class<? extends Annotation> bindingType) {
        checkWithinObserverNotification();
        getTypeStore().add(bindingType, QualifierLiteral.INSTANCE);
        getBeanManager().getServices().get(ClassTransformer.class).clearAnnotationData(bindingType);
        getBeanManager().getServices().get(MetaAnnotationStore.class).clearAnnotationData(bindingType);
        BootstrapLogger.LOG.addQualifierCalled(getReceiver(), bindingType);
    }

    @Override
    public void addInterceptorBinding(Class<? extends Annotation> bindingType, Annotation... bindingTypeDef) {
        checkWithinObserverNotification();
        TypeStore typeStore = getTypeStore();
        typeStore.add(bindingType, InterceptorBindingTypeLiteral.INSTANCE);
        for (Annotation a : bindingTypeDef) {
            typeStore.add(bindingType, a);
        }
        getBeanManager().getServices().get(ClassTransformer.class).clearAnnotationData(bindingType);
        getBeanManager().getServices().get(MetaAnnotationStore.class).clearAnnotationData(bindingType);
        BootstrapLogger.LOG.addInterceptorBindingCalled(getReceiver(), bindingType);
    }

    @Override
    public void addScope(Class<? extends Annotation> scopeType, boolean normal, boolean passivating) {
        checkWithinObserverNotification();
        if (normal) {
            getTypeStore().add(scopeType, new NormalScopeLiteral(passivating));
        } else if (passivating) {
            throw BootstrapLogger.LOG.passivatingNonNormalScopeIllegal(scopeType);
        } else {
            getTypeStore().add(scopeType, ScopeLiteral.INSTANCE);
        }
        getBeanManager().getServices().get(ClassTransformer.class).clearAnnotationData(scopeType);
        getBeanManager().getServices().get(MetaAnnotationStore.class).clearAnnotationData(scopeType);
        getBeanManager().getServices().get(ReflectionCache.class).cleanup();
        BootstrapLogger.LOG.addScopeCalled(getReceiver(), scopeType);
    }

    @Override
    public void addStereotype(Class<? extends Annotation> stereotype, Annotation... stereotypeDef) {
        checkWithinObserverNotification();
        TypeStore typeStore = getTypeStore();
        typeStore.add(stereotype, StereotypeLiteral.INSTANCE);
        for (Annotation a : stereotypeDef) {
            typeStore.add(stereotype, a);
        }
        getBeanManager().getServices().get(ClassTransformer.class).clearAnnotationData(stereotype);
        getBeanManager().getServices().get(MetaAnnotationStore.class).clearAnnotationData(stereotype);
        BootstrapLogger.LOG.addStereoTypeCalled(getReceiver(), stereotype);
    }

    @Override
    public void addAnnotatedType(AnnotatedType<?> source) {
        checkWithinObserverNotification();
        BootstrapLogger.LOG.deprecatedAddAnnotatedTypeMethodUsed(source.getJavaClass());
        addAnnotatedType(source, null);
    }

    @Override
    public void addAnnotatedType(AnnotatedType<?> type, String id) {
        checkWithinObserverNotification();
        addSyntheticAnnotatedType(type, id);
        BootstrapLogger.LOG.addAnnotatedTypeCalledInBBD(getReceiver(), type);
    }

    @Override
    public <T> AnnotatedTypeConfigurator<T> addAnnotatedType(String id, Class<T> type) {
        checkWithinObserverNotification();
        AnnotatedTypeConfiguratorImpl<T> configurator = new AnnotatedTypeConfiguratorImpl<>(getBeanManager().createAnnotatedType(type));
        additionalAnnotatedTypes.add(new AnnotatedTypeRegistration<T>(configurator, id));
        return configurator;
    }

    @Override
    public void addQualifier(AnnotatedType<? extends Annotation> qualifier) {
        checkWithinObserverNotification();
        addSyntheticAnnotation(qualifier, QualifierLiteral.INSTANCE);
        BootstrapLogger.LOG.addQualifierCalled(getReceiver(), qualifier);
    }

    @Override
    public void addInterceptorBinding(AnnotatedType<? extends Annotation> bindingType) {
        checkWithinObserverNotification();
        addSyntheticAnnotation(bindingType, InterceptorBindingTypeLiteral.INSTANCE);
        BootstrapLogger.LOG.addInterceptorBindingCalled(getReceiver(), bindingType);
    }

    private <A extends Annotation> void addSyntheticAnnotation(AnnotatedType<A> annotation, Annotation requiredMetaAnnotation) {
        if (requiredMetaAnnotation != null && !annotation.isAnnotationPresent(requiredMetaAnnotation.annotationType())) {
            // Add required meta annotation
            annotation = new AnnotatedTypeWrapper<A>(annotation, requiredMetaAnnotation);
        }
        getBeanManager().getServices().get(ClassTransformer.class).addSyntheticAnnotation(annotation, getBeanManager().getId());
        getBeanManager().getServices().get(MetaAnnotationStore.class).clearAnnotationData(annotation.getJavaClass());
    }

}
