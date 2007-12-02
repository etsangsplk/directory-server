/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.directory.server.core.integ.state;


import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.integ.DirectoryServiceFactory;
import org.apache.directory.server.core.integ.ServiceScope;
import org.apache.directory.server.core.integ.InheritableSettings;
import org.junit.runner.notification.RunNotifier;
import org.junit.internal.runners.TestClass;
import org.junit.internal.runners.TestMethod;

import javax.naming.NamingException;
import java.io.IOException;


/**
 * The context for managing the state of an integration test service.
 * Each thread of execution driving tests manages it's own service context.
 * Hence parallelism can be achieved while running integration tests.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class TestServiceContext
{
    private static final ThreadLocal<TestServiceContext> CONTEXTS = new ThreadLocal<TestServiceContext>();

    private final TestServiceState nonExistentState = new NonExistentState( this );
    private final TestServiceState startedDirtyState = new StartedDirtyState( this );
    private final TestServiceState startedPristineState = new StartedPristineState( this );
    private final TestServiceState startedRevertedState = new StartedRevertedState( this );
    private final TestServiceState stoppedDirtyState = new StoppedDirtyState( this );
    private final TestServiceState stoppedPristineState = new StoppedPristineState( this );


    /**
     * the level at which the service was started and where it will be 
     * shutdown, cleaned and destroyed if it is still present
     */
    private final ServiceScope scope;

    /** current service state with respect to the testing life cycle */
    private TestServiceState state;

    /** the core directory service managed by this context */
    private DirectoryService service;


    public TestServiceContext( ServiceScope scope )
    {
        this.scope = scope;
    }


    /**
     * Gets the TestServiceContext associated with the current thread of
     * execution.  If one does not yet exist it will be created using the
     * provided scope parameter.  If the scope is null a null pointer
     * exception may result.
     *
     * @param scope the level at which the service was started
     * @return the context associated with the calling thread
     */
    public static TestServiceContext get( ServiceScope scope )
    {
        TestServiceContext context = CONTEXTS.get();

        if ( context == null )
        {
            context = new TestServiceContext( scope );
            CONTEXTS.set( context );
        }

        return context;
    }
    

    /**
     * Sets the TestServiceContext for this current thread
     *
     * @param context the context associated with the calling thread
     */
    public static void set( TestServiceContext context )
    {
        CONTEXTS.set( context );
    }


    /**
     * Action where an attempt is made to create the service.  Service
     * creation in this system is the combined instantiation and
     * configuration which takes place when the factory is used to get
     * a new instance of the service.
     *
     * @param factory the factory to use for creating a configured service
     */
    public void create( DirectoryServiceFactory factory )
    {
        state.create( factory );
    }


    /**
     * Action where an attempt is made to destroy the service.  This
     * entails nulling out reference to it and triggering garbage
     * collection.
     */
    public void destroy()
    {
        state.destroy();
    }


    /**
     * Action where an attempt is made to erase the contents of the
     * working directory used by the service for various files including
     * partition database files.
     *
     * @throws IOException on errors while deleting the working directory
     */
    public void cleanup() throws IOException
    {
        state.cleanup();
    }


    /**
     * Action where an attempt is made to start up the service.
     *
     * @throws NamingException on failures to start the core directory service
     */
    public void startup() throws NamingException
    {
        state.startup();
    }


    /**
     * Action where an attempt is made to shutdown the service.
     *
     * @throws NamingException on failures to stop the core directory service
     */
    public void shutdown() throws NamingException
    {
        state.shutdown();
    }


    /**
     * Action where an attempt is made to run a test against the service.
     *
     * All annotations should have already been processed for
     * InheritableSettings yet they and others can be processed since we have
     * access to the method annotations below
     *
     * @param testClass the class whose test method is to be run
     * @param testMethod the test method which is to be run
     * @param notifier a notifier to report failures to
     * @param settings the inherited settings and annotations associated with
     * the test method
     */
    public void test( TestClass testClass, TestMethod testMethod, RunNotifier notifier, InheritableSettings settings )
    {
        state.test( testClass, testMethod, notifier, settings );
    }


    /**
     * Action where an attempt is made to revert the service to it's
     * initial start up state by using a previous snapshot.
     *
     * @throws NamingException on failures to revert the state of the core
     * directory service
     */
    public void revert() throws NamingException
    {
        state.revert();
    }


    void setState( TestServiceState state )
    {
        this.state = state;
    }


    TestServiceState getState()
    {
        return state;
    }


    TestServiceState getNonExistentState()
    {
        return nonExistentState;
    }


    TestServiceState getStartedDirtyState()
    {
        return startedDirtyState;
    }


    TestServiceState getStartedPristineState()
    {
        return startedPristineState;
    }


    TestServiceState getStartedRevertedState()
    {
        return startedRevertedState;
    }


    TestServiceState getStoppedDirtyState()
    {
        return stoppedDirtyState;
    }


    TestServiceState getStoppedPristineState()
    {
        return stoppedPristineState;
    }


    ServiceScope getScope()
    {
        return scope;
    }


    DirectoryService getService()
    {
        return service;
    }


    void setService( DirectoryService service )
    {
        this.service = service;
    }
}
