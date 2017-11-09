/*
 * Copyright (c) 2002-2014, Mairie de Paris
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice
 *     and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice
 *     and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 *  3. Neither the name of 'Mairie de Paris' nor 'Lutece' nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * License 1.0
 */
package fr.paris.lutece.plugins.blog.business.portlet;

import java.util.Collection;
import java.util.Map;

import fr.paris.lutece.portal.business.portlet.IPortletInterfaceDAO;
import fr.paris.lutece.portal.business.portlet.PortletHome;
import fr.paris.lutece.portal.business.portlet.PortletTypeHome;
import fr.paris.lutece.portal.service.spring.SpringContextService;
import fr.paris.lutece.util.ReferenceItem;

/**
 * This class provides instances management methods for ArticlesListPortlet objects
 */
public class HtmlDocsListPortletHome extends PortletHome
{
    // ///////////////////////////////////////////////////////////////////////////////
    // Constants
    // Static variable pointed at the DAO instance
    private static IHtmlDocsListPortletDAO _dao = SpringContextService.getBean( "blog.htmlDocsListPortletDAO" );

    /* This class implements the Singleton design pattern. */
    private static HtmlDocsListPortletHome _singleton = null;

    /**
     * Constructor
     */
    public HtmlDocsListPortletHome( )
    {
        if ( _singleton == null )
        {
            _singleton = this;
        }
    }

    /**
     * Returns the instance of DocumentListPortletHome
     *
     * @return the DocumentListPortletHome instance
     */
    public static PortletHome getInstance( )
    {
        if ( _singleton == null )
        {
            _singleton = new HtmlDocsListPortletHome( );
        }

        return _singleton;
    }

    /**
     * Returns the identifier of the portlet type
     *
     * @return the portlet type identifier
     */
    public String getPortletTypeId( )
    {
        String strCurrentClassName = this.getClass( ).getName( );
        String strPortletTypeId = PortletTypeHome.getPortletTypeId( strCurrentClassName );

        return strPortletTypeId;
    }

    /**
     * Returns the instance of the portlet DAO singleton
     *
     * @return the instance of the DAO singleton
     */
    public IPortletInterfaceDAO getDAO( )
    {
        return _dao;
    }

    /**
     * Load the list of documentTypes
     * 
     * @param nDocumentId
     *            the document ID
     * @param strCodeDocumentType
     *            The code
     * @param pOrder
     *            order of the portlets
     * @param pFilter
     *            The portlet filter
     * @return The Collection of the ReferenceItem
     */
    /*
     * public static Collection<ReferenceItem> findByCodeDocumentTypeAndCategory( int nDocumentId, String strCodeDocumentType, PortletOrder pOrder,
     * PortletFilter pFilter ) { //FIXME : method should access to different home business methods return _dao.selectByDocumentIdAndDocumentType( nDocumentId,
     * strCodeDocumentType, pOrder, pFilter ); }
     */

    /**
     * Check whether a portlet is an alias portlet
     * 
     * @param nPortletId
     *            The id of the portlet
     * @return True if the portlet is an alias portlet, false otherwise
     */
    public static boolean checkIsAliasPortlet( int nPortletId )
    {
        return _dao.checkIsAliasPortlet( nPortletId );
    }

    /**
     * Load the list of Portlet
     * 
     * @param nDocumentId
     *            the document ID
     * @param pOrder
     *            order of the portlets
     * @param pFilter
     *            The portlet filter
     * @return The Collection of the ReferenceItem
     */
    public static Collection<ReferenceItem> findByFilter( int nDocumentId, PortletOrder pOrder, PortletFilter pFilter )
    {
        // FIXME : method should access to different home business methods
        return _dao.selectPortletByType( nDocumentId, pOrder, pFilter );
    }
    
    /**
     * Load the portlet template whose type is specified in parameter
     * @param strPortletType
     * @return Map template
     */
    public static Map<Integer, String> loadPages( String strPortletType )
    {
        return _dao.loadPages( strPortletType );
    }
}
