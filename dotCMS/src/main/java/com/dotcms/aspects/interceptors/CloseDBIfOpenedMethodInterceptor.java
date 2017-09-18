package com.dotcms.aspects.interceptors;

import com.dotcms.aspects.DelegateMethodInvocation;
import com.dotcms.aspects.MethodInterceptor;
import com.dotcms.business.CloseDBIfOpened;
import com.dotmarketing.db.DbConnectionFactory;

import static com.dotcms.util.AnnotationUtils.getMethodAnnotation;

/**
 * Method handler for the {@link CloseDBIfOpened} annotation aspect
 * @author jsanca
 */
public class CloseDBIfOpenedMethodInterceptor implements MethodInterceptor<Object> {

    public static final CloseDBIfOpenedMethodInterceptor INSTANCE = new CloseDBIfOpenedMethodInterceptor();


    @Override
    public Object invoke(final DelegateMethodInvocation<Object> delegate) throws Throwable {
        return delegate.proceed();
    } // invoke.
} // E:O:F:LogTimeMethodInterceptor.