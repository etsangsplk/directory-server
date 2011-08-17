/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.server.xdbm.search.impl;


import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.xdbm.AbstractIndexCursor;
import org.apache.directory.server.xdbm.ForwardIndexEntry;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.IndexCursor;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.shared.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.schema.AttributeType;


/**
 * A Cursor over entry candidates matching a LessEq assertion filter.  This
 * Cursor operates in two modes.  The first is when an index exists for the
 * attribute the assertion is built on.  The second is when the user index for
 * the assertion attribute does not exist.  Different Cursors are used in each
 * of these cases where the other remains null.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LessEqCursor<V, ID extends Comparable<ID>> extends AbstractIndexCursor<V, Entry, ID>
{
    private static final String UNSUPPORTED_MSG = I18n.err( I18n.ERR_716 );

    /** An less eq evaluator for candidates */
    private final LessEqEvaluator<V, ID> lessEqEvaluator;

    /** Cursor over attribute entry matching filter: set when index present */
    private final IndexCursor<V, Entry, ID> userIdxCursor;

    /** NDN Cursor on all entries in  (set when no index on user attribute) */
    private final IndexCursor<V, Entry, ID> uuidIdxCursor;

    /**
     * Used to store indexEntry from ndnCandidate so it can be saved after
     * call to evaluate() which changes the value so it's not referring to
     * the NDN but to the value of the attribute instead.
     */
    IndexEntry<V, Entry, ID> ndnCandidate;


    @SuppressWarnings("unchecked")
    public LessEqCursor( Store<Entry, ID> db, LessEqEvaluator<V, ID> lessEqEvaluator ) throws Exception
    {
        this.lessEqEvaluator = lessEqEvaluator;

        AttributeType attributeType = lessEqEvaluator.getExpression().getAttributeType();
        
        if ( db.hasIndexOn( attributeType ) )
        {
            userIdxCursor = ( ( Index<V, Entry, ID> ) db.getIndex( attributeType ) ).forwardCursor();
            uuidIdxCursor = null;
        }
        else
        {
            uuidIdxCursor = ( IndexCursor<V, Entry, ID> ) db.getEntryUuidIndex().forwardCursor();
            userIdxCursor = null;
        }
    }


    /**
     * {@inheritDoc}
     */
    protected String getUnsupportedMessage()
    {
        return UNSUPPORTED_MSG;
    }

    
    public void beforeValue( ID id, V value ) throws Exception
    {
        checkNotClosed( "beforeValue()" );
        
        if ( userIdxCursor != null )
        {
            /*
             * First we need to check and make sure this element is within
             * bounds as mandated by the assertion node.  To do so we compare
             * it's value with the value of the expression node.  If the
             * element's value is greater than this upper bound then we
             * position the userIdxCursor after the last node.
             *
             * If the element's value is equal to this upper bound then we
             * position the userIdxCursor right before the last node.
             *
             * If the element's value is smaller, then we delegate to the
             * before() method of the userIdxCursor.
             */
            //noinspection unchecked
            int compareValue = lessEqEvaluator.getComparator().compare( value,
                lessEqEvaluator.getExpression().getValue().getValue() );

            if ( compareValue > 0 )
            {
                afterLast();
                return;
            }
            else if ( compareValue == 0 )
            {
                last();
                previous();
                setAvailable( false );
                return;
            }

            userIdxCursor.beforeValue( id, value );
            setAvailable( false );
        }
        else
        {
            throw new UnsupportedOperationException( UNSUPPORTED_MSG );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void before( IndexEntry<V, Entry, ID> element ) throws Exception
    {
        checkNotClosed( "before()" );
        
        if ( userIdxCursor != null )
        {
            /*
             * First we need to check and make sure this element is within
             * bounds as mandated by the assertion node.  To do so we compare
             * it's value with the value of the expression node.  If the
             * element's value is greater than this upper bound then we
             * position the userIdxCursor after the last node.
             *
             * If the element's value is equal to this upper bound then we
             * position the userIdxCursor right before the last node.
             *
             * If the element's value is smaller, then we delegate to the
             * before() method of the userIdxCursor.
             */
            int compareValue = lessEqEvaluator.getComparator().compare( element.getValue(),
                lessEqEvaluator.getExpression().getValue().getValue() );

            if ( compareValue > 0 )
            {
                afterLast();
                return;
            }
            else if ( compareValue == 0 )
            {
                last();
                previous();
                setAvailable( false );
                return;
            }

            userIdxCursor.before( element );
            setAvailable( false );
        }
        else
        {
            super.before( element );
        }
    }


    public void afterValue( ID id, V value ) throws Exception
    {
        checkNotClosed( "afterValue()" );
        if ( userIdxCursor != null )
        {
            int comparedValue = lessEqEvaluator.getComparator().compare( value,
                lessEqEvaluator.getExpression().getValue().getValue() );

            /*
             * First we need to check and make sure this element is within
             * bounds as mandated by the assertion node.  To do so we compare
             * it's value with the value of the expression node.
             *
             * If the element's value is equal to or greater than this upper
             * bound then we position the userIdxCursor after the last node.
             *
             * If the element's value is smaller, then we delegate to the
             * after() method of the userIdxCursor.
             */
            if ( comparedValue >= 0 )
            {
                afterLast();
                return;
            }

            // Element is in the valid range as specified by assertion
            userIdxCursor.afterValue( id, value );
            setAvailable( false );
        }
        else
        {
            throw new UnsupportedOperationException( UNSUPPORTED_MSG );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void after( IndexEntry<V, Entry, ID> element ) throws Exception
    {
        checkNotClosed( "after()" );
        
        if ( userIdxCursor != null )
        {
            int comparedValue = lessEqEvaluator.getComparator().compare( element.getValue(),
                lessEqEvaluator.getExpression().getValue().getValue() );

            /*
             * First we need to check and make sure this element is within
             * bounds as mandated by the assertion node.  To do so we compare
             * it's value with the value of the expression node.
             *
             * If the element's value is equal to or greater than this upper
             * bound then we position the userIdxCursor after the last node.
             *
             * If the element's value is smaller, then we delegate to the
             * after() method of the userIdxCursor.
             */
            if ( comparedValue >= 0 )
            {
                afterLast();
                return;
            }

            // Element is in the valid range as specified by assertion
            userIdxCursor.after( element );
            setAvailable( false );
        }
        else
        {
            super.after( element );
        }
    }


    public void beforeFirst() throws Exception
    {
        checkNotClosed( "beforeFirst()" );
        if ( userIdxCursor != null )
        {
            userIdxCursor.beforeFirst();
        }
        else
        {
            uuidIdxCursor.beforeFirst();
            ndnCandidate = null;
        }

        setAvailable( false );
    }


    public void afterLast() throws Exception
    {
        checkNotClosed( "afterLast()" );
        if ( userIdxCursor != null )
        {
            IndexEntry<V, Entry, ID> advanceTo = new ForwardIndexEntry<V, Entry, ID>();
            //noinspection unchecked
            advanceTo.setValue( ( V ) lessEqEvaluator.getExpression().getValue().getValue() );
            userIdxCursor.after( advanceTo );
        }
        else
        {
            uuidIdxCursor.afterLast();
            ndnCandidate = null;
        }

        setAvailable( false );
    }


    public boolean first() throws Exception
    {
        beforeFirst();
        return next();
    }


    public boolean last() throws Exception
    {
        afterLast();
        return previous();
    }


    public boolean previous() throws Exception
    {
        checkNotClosed( "previous()" );

        if ( userIdxCursor != null )
        {
            /*
             * No need to do the same check that is done in next() since
             * values are decreasing with calls to previous().  We will
             * always have lesser values.
             */
            return setAvailable( userIdxCursor.previous() );
        }

        while ( uuidIdxCursor.previous() )
        {
            checkNotClosed( "previous()" );
            ndnCandidate = uuidIdxCursor.get();
            if ( lessEqEvaluator.evaluate( ndnCandidate ) )
            {
                return setAvailable( true );
            }
            else
            {
                ndnCandidate = null;
            }
        }

        return setAvailable( false );
    }


    public boolean next() throws Exception
    {
        checkNotClosed( "next()" );
        
        if ( userIdxCursor != null )
        {
            /*
             * We have to check and make sure the next value complies by
             * being less than or eq to the expression node's value.  We need
             * to do this since values are increasing and we must limit to our
             * upper bound.
             */
            while ( userIdxCursor.next() )
            {
                checkNotClosed( "next()" );
                IndexEntry<?, Entry, ID> candidate = userIdxCursor.get();
                
                if ( lessEqEvaluator.getComparator().compare( candidate.getValue(),
                    lessEqEvaluator.getExpression().getValue().getValue() ) <= 0 )
                {
                    return setAvailable( true );
                }
            }

            return setAvailable( false );
        }

        while ( uuidIdxCursor.next() )
        {
            checkNotClosed( "next()" );
            ndnCandidate = uuidIdxCursor.get();
            
            if ( lessEqEvaluator.evaluate( ndnCandidate ) )
            {
                return setAvailable( true );
            }
            else
            {
                ndnCandidate = null;
            }
        }

        return setAvailable( false );
    }


    public IndexEntry<V, Entry, ID> get() throws Exception
    {
        checkNotClosed( "get()" );
        
        if ( userIdxCursor != null )
        {
            if ( available() )
            {
                return userIdxCursor.get();
            }

            throw new InvalidCursorPositionException( I18n.err( I18n.ERR_708 ) );
        }

        if ( available() )
        {
            return ndnCandidate;
        }

        throw new InvalidCursorPositionException( I18n.err( I18n.ERR_708 ) );
    }


    public void close() throws Exception
    {
        super.close();

        if ( userIdxCursor != null )
        {
            userIdxCursor.close();
        }
        else
        {
            uuidIdxCursor.close();
            ndnCandidate = null;
        }
    }
}