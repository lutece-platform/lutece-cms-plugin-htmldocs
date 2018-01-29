/*
 * Copyright (c) 2002-2016, Mairie de Paris
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
package fr.paris.lutece.plugins.blog.web;

import fr.paris.lutece.plugins.blog.business.DocContent;
import fr.paris.lutece.plugins.blog.business.Blog;
import fr.paris.lutece.plugins.blog.business.BlogHome;
import fr.paris.lutece.plugins.blog.business.BlogSearchFilter;
import fr.paris.lutece.plugins.blog.business.Tag;
import fr.paris.lutece.plugins.blog.business.TagHome;
import fr.paris.lutece.plugins.blog.business.portlet.BlogPublication;
import fr.paris.lutece.plugins.blog.business.portlet.BlogPublicationHome;
import fr.paris.lutece.plugins.blog.service.BlogService;
import fr.paris.lutece.plugins.blog.service.docsearch.BlogSearchService;
import fr.paris.lutece.portal.service.message.AdminMessage;
import fr.paris.lutece.portal.service.message.AdminMessageService;
import fr.paris.lutece.portal.util.mvc.admin.annotations.Controller;
import fr.paris.lutece.portal.util.mvc.commons.annotations.Action;
import fr.paris.lutece.portal.util.mvc.commons.annotations.View;
import fr.paris.lutece.portal.web.resource.ExtendableResourcePluginActionManager;
import fr.paris.lutece.portal.web.upload.MultipartHttpServletRequest;
import fr.paris.lutece.portal.web.util.LocalizedPaginator;
import fr.paris.lutece.util.html.Paginator;
import fr.paris.lutece.util.json.JsonResponse;
import fr.paris.lutece.util.json.JsonUtil;
import fr.paris.lutece.util.sort.AttributeComparator;
import fr.paris.lutece.util.url.UrlItem;
import fr.paris.lutece.util.ReferenceItem;
import fr.paris.lutece.util.ReferenceList;
import fr.paris.lutece.portal.service.rbac.RBACService;
import fr.paris.lutece.portal.service.resource.ExtendableResourceRemovalListenerService;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.portal.service.util.AppPathService;
import fr.paris.lutece.portal.service.util.AppPropertiesService;
import fr.paris.lutece.portal.service.admin.AccessDeniedException;
import fr.paris.lutece.portal.service.admin.AdminUserService;
import fr.paris.lutece.portal.service.datastore.DatastoreService;
import fr.paris.lutece.portal.business.rbac.RBAC;
import fr.paris.lutece.portal.business.user.AdminUser;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URLConnection;

import javax.servlet.http.HttpServletRequest;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.outerj.daisy.diff.HtmlCleaner;
import org.outerj.daisy.diff.html.HTMLDiffer;
import org.outerj.daisy.diff.html.HtmlSaxDiffOutput;
import org.outerj.daisy.diff.html.TextNodeComparator;
import org.outerj.daisy.diff.html.dom.DomTreeBuilder;
import org.xml.sax.InputSource;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

/**
 * This class provides the user interface to manage Blog features ( manage, create, modify, remove )
 */
@Controller( controllerJsp = "ManageBlogs.jsp", controllerPath = "jsp/admin/plugins/blog/", right = "BLOG_MANAGEMENT" )
public class BlogJspBean extends ManageBlogJspBean
{
    // Templates
    private static final String TEMPLATE_MANAGE_BLOGS = "/admin/plugins/blog/manage_blogs.html";
    private static final String TEMPLATE_CREATE_BLOG = "/admin/plugins/blog/create_blog.html";
    private static final String TEMPLATE_MODIFY_BLOG = "/admin/plugins/blog/modify_blog.html";
    private static final String TEMPLATE_HISTORY_BLOG = "admin/plugins/blog/history_blog.html";
    private static final String TEMPLATE_PREVIEW_BLOG = "admin/plugins/blog/preview_blog.html";
    private static final String TEMPLATE_DIFF_BLOG = "admin/plugins/blog/diff_blog.html";

    // Parameters
    protected static final String PARAMETER_ID_BLOG = "id";
    protected static final String PARAMETER_VERSION_BLOG = "blog_version";
    protected static final String PARAMETER_VERSION_BLOG2 = "blog_version2";
    protected static final String PARAMETER_CONTENT_LABEL = "content_label";
    protected static final String PARAMETER_HTML_CONTENT = "html_content";
    protected static final String PARAMETER_EDIT_COMMENT = "edit_comment";
    protected static final String PARAMETER_DESCRIPTION = "description";
    protected static final String PARAMETER_VIEW = "view";
    protected static final String PARAMETER_BUTTON_SEARCH = "button_search";
    protected static final String PARAMETER_SEARCH_TEXT = "search_text";
    protected static final String PARAMETER_UPDATE_ATTACHMENT = "update_attachment";
    protected static final String PARAMETER_TAG = "tag_doc";
    protected static final String PARAMETER_TAG_NAME = "tag_name";
    protected static final String PARAMETER_URL= "url";

    protected static final String PARAMETER_TAG_TO_REMOVE = "tag_remove";
    protected static final String PARAMETER_SHAREABLE = "shareable";
    protected static final String PARAMETER_PRIORITY = "tag_priority";
    protected static final String PARAMETER_TAG_ACTION = "tagAction";
    protected static final String PARAMETER_ACTION_BUTTON = "button";
    protected static final String PARAMETER_APPLY = "apply";


    // Properties for page titles
    private static final String PROPERTY_PAGE_TITLE_MANAGE_BLOG = "blog.manage_blog.pageTitle";
    private static final String PROPERTY_PAGE_TITLE_MODIFY_BLOG = "blog.modify_blog.pageTitle";
    private static final String PROPERTY_PAGE_TITLE_CREATE_BLOG = "blog.create_blog.pageTitle";
    private static final String PROPERTY_PAGE_TITLE_HISTORY_BLOG = "blog.history_blog.pageTitle";
    private static final String PROPERTY_PAGE_TITLE_PREVIEW_BLOG = "blog.preview_blog.pageTitle";
    private static final String PROPERTY_PAGE_TITLE_DIFF_BLOG = "blog.diff_blog.pageTitle";
    protected static final String PROPERTY_USE_UPLOAD_IMAGE_PLUGIN = "use_upload_image_plugin";

    // Properties
    private static final String PROPERTY_DEFAULT_LIST_ITEM_PER_PAGE = "blog.listItems.itemsPerPage";

    // Markers
    protected static final String MARK_BLOG_LIST = "blog_list";
    protected static final String MARK_BLOG_VERSION_LIST = "blog_version_list";
    protected static final String MARK_BLOG = "blog";
    protected static final String MARK_WEBAPP_URL = "webapp_url";
    protected static final String MARK_IS_CHECKED = "is_checked";
    protected static final String MARK_CURRENT_USER = "current_user";
    protected static final String MARK_ID_BLOG = "id";
    protected static final String MARK_SEARCH_TEXT = "search_text";
    protected static final String MARK_DIFF = "blog_diff";
    protected static final String MARK_BLOG2 = "blog2";
    protected static final String MARK_LIST_TAG = "list_tag";
    protected static final String MARK_SORTED_ATTRIBUTE = "sorted_attribute_name";
    protected static final String MARK_TAG = "tags";
    protected static final String MARK_USE_UPLOAD_IMAGE_PLUGIN = "is_crop_image";
    protected static final String MARK_PERMISSION_CREATE_BLOG = "permission_manage_create_blog";
    protected static final String MARK_PERMISSION_MODIFY_BLOG = "permission_manage_modify_blog";
    protected static final String MARK_PERMISSION_PUBLISH_BLOG = "permission_manage_publish_blog";
    protected static final String MARK_PERMISSION_DELETE_BLOG = "permission_manage_delete_blog";
    

 

    private static final String JSP_MANAGE_BLOGS = "jsp/admin/plugins/blog/ManageBlogs.jsp";

    // Properties
    private static final String MESSAGE_CONFIRM_REMOVE_BLOG = "blog.message.confirmRemoveBlog";
    private static final String MESSAGE_ERROR_DOCUMENT_IS_PUBLISHED = "blog.message.errorDocumentIsPublished";

    // Validations
    private static final String VALIDATION_ATTRIBUTES_PREFIX = "blog.model.entity.blog.attribute.";

    // Views
    protected static final String VIEW_MANAGE_BLOGS = "manageBlogs";
    private static final String VIEW_CREATE_BLOG = "createBlog";
    private static final String VIEW_MODIFY_BLOG = "modifyBlog";
    private static final String VIEW_HISTORY_BLOG = "historyBlog";
    private static final String VIEW_PREVIEW_BLOG = "previewBlog";
    private static final String VIEW_DIFF_BLOG = "diffBlog";

    // Actions
    private static final String ACTION_CREATE_BLOG = "createBlog";
    private static final String ACTION_MODIFY_BLOG = "modifyBlog";
    private static final String ACTION_REMOVE_BLOG = "removeBlog";
    private static final String ACTION_CONFIRM_REMOVE_BLOG = "confirmRemoveBlog";
    private static final String ACTION_ADD_TAG = "addTag";
    private static final String ACTION_REMOVE_TAG = "removeTag";
    private static final String ACTION_UPDATE_PRIORITY_TAG = "updatePriorityTag";
    private static final String ACTION_ADD_FILE_CONTENT = "addContent";
    private static final String ACTION_REMOVE_FILE_CONTENT = "deleteContent";

    // Infos
    private static final String INFO_BLOG_CREATED = "blog.info.blog.created";
    private static final String INFO_BLOG_UPDATED = "blog.info.blog.updated";
    private static final String INFO_BLOG_REMOVED = "blog.info.blog.removed";

    // Filter Marks
    protected static final String MARK_BLOG_FILTER_LIST = "blog_filter_list";
    protected static final String MARK_BLOG_FILTER_NAME = "Nom";
    protected static final String MARK_BLOG_FILTER_DATE = "Date";
    protected static final String MARK_BLOG_FILTER_USER = "Utilisateur";
    protected static final String MARK_PAGINATOR = "paginator";
    protected static final String MARK_NB_ITEMS_PER_PAGE = "nb_items_per_page";
    protected static final String MARK_ASC_SORT = "asc_sort";


    // Session variable to store working values
    protected Blog _blog;
    protected boolean _bIsChecked = false;
    protected String _strSearchText;
    protected String _strCurrentPageIndex;
    protected int _nItemsPerPage;
    protected int _nDefaultItemsPerPage;
    protected boolean _bIsSorted = false;
    protected String _strSortedAttributeName;
    protected Boolean _bIsAscSort;
    protected String [ ] _strTag;

    /**
     * Build the Manage View
     * 
     * @param request
     *            The HTTP request
     * @return The page
     */
    @View( value = VIEW_MANAGE_BLOGS, defaultView = true )
    public String getManageBlogs( HttpServletRequest request )
    {
    	_blog = null;
        _strCurrentPageIndex = Paginator.getPageIndex( request, Paginator.PARAMETER_PAGE_INDEX, _strCurrentPageIndex );
        _nDefaultItemsPerPage = AppPropertiesService.getPropertyInt( PROPERTY_DEFAULT_LIST_ITEM_PER_PAGE, 50 );
        _nItemsPerPage = Paginator.getItemsPerPage( request, Paginator.PARAMETER_ITEMS_PER_PAGE, _nItemsPerPage, _nDefaultItemsPerPage );

        // SORT
        String strSortedAttributeName = request.getParameter( MARK_SORTED_ATTRIBUTE );
        String strAscSort = null;

        AdminUser user = AdminUserService.getAdminUser( request );
        List<Integer> listBlogsId = new ArrayList<Integer>( );
        String strButtonSearch = request.getParameter( PARAMETER_BUTTON_SEARCH );
        if ( strButtonSearch != null )
        {
            // CURRENT USER
            _bIsChecked = request.getParameter( MARK_CURRENT_USER ) != null;
            _strSearchText = request.getParameter( PARAMETER_SEARCH_TEXT );
            _strTag = request.getParameterValues( PARAMETER_TAG );
        }

        if ( StringUtils.isNotBlank( _strSearchText ) || ( _strTag != null && _strTag.length > 0 ) || _bIsChecked )
        {
            BlogSearchFilter filter = new BlogSearchFilter( );
            if ( StringUtils.isNotBlank( _strSearchText ) )
                filter.setKeywords( _strSearchText );
            if ( _strTag != null && ( _strTag.length > 0 ) )
                filter.setTag( _strTag );
            if ( _bIsChecked )
                filter.setUser( user.getFirstName( ) );
            BlogSearchService.getInstance( ).getSearchResults( filter, listBlogsId );

        }

        else
        {

        	listBlogsId = BlogHome.getIdBlogsList( );
        }

        LocalizedPaginator<Integer> paginator = new LocalizedPaginator<Integer>( (List<Integer>) listBlogsId, _nItemsPerPage, getHomeUrl( request ),
                Paginator.PARAMETER_PAGE_INDEX, _strCurrentPageIndex, getLocale( ) );

        List<Blog> listDocuments = new ArrayList<Blog>( );

        for ( Integer documentId : paginator.getPageItems( ) )
        {
            Blog document = BlogService.getInstance( ).findByPrimaryKeyWithoutBinaries( documentId );

            if ( document != null )
            {
                listDocuments.add( document );
            }
        }

        if ( strSortedAttributeName != null || _bIsSorted == true )
        {
            if ( strSortedAttributeName == null )
            {
                strSortedAttributeName = _strSortedAttributeName;
            }
            strAscSort = request.getParameter( MARK_ASC_SORT );

            boolean bIsAscSort = Boolean.parseBoolean( strAscSort );
            if ( strAscSort == null )
            {
                bIsAscSort = _bIsAscSort;
            }

            Collections.sort( listDocuments, new AttributeComparator( strSortedAttributeName, bIsAscSort ) );

            _bIsSorted = true;

            _strSortedAttributeName = strSortedAttributeName;
            _bIsAscSort = bIsAscSort;
        }
        boolean bPermissionCreate = RBACService.isAuthorized( Blog.PROPERTY_RESOURCE_TYPE, RBAC.WILDCARD_RESOURCES_ID,
                Blog.PERMISSION_CREATE, getUser( ) );
        boolean bPermissionModify = RBACService.isAuthorized( Blog.PROPERTY_RESOURCE_TYPE, RBAC.WILDCARD_RESOURCES_ID,
                Blog.PERMISSION_MODIFY, getUser( ) );
        boolean bPermissionDelete = RBACService.isAuthorized( Blog.PROPERTY_RESOURCE_TYPE, RBAC.WILDCARD_RESOURCES_ID,
                Blog.PERMISSION_DELETE, getUser( ) );
        boolean bPermissionPublish = RBACService.isAuthorized( Blog.PROPERTY_RESOURCE_TYPE, RBAC.WILDCARD_RESOURCES_ID,
                Blog.PERMISSION_PUBLISH, getUser( ) );

        Map<String, Object> model = new HashMap<String, Object>( );
        model.put( MARK_BLOG_LIST, listDocuments );
        model.put( MARK_PAGINATOR, paginator );
        model.put( MARK_BLOG_FILTER_LIST, getBlogFilterList( ) );
        model.put( MARK_LIST_TAG, TagHome.getTagsReferenceList( ) );
        model.put( MARK_WEBAPP_URL, AppPathService.getBaseUrl( request ) );
        model.put( MARK_IS_CHECKED, _bIsChecked );
        model.put( MARK_SEARCH_TEXT, _strSearchText );
        model.put( MARK_NB_ITEMS_PER_PAGE, "" + _nItemsPerPage );
        model.put( MARK_TAG, _strTag );
        
        model.put( MARK_PERMISSION_CREATE_BLOG, bPermissionCreate );
        model.put( MARK_PERMISSION_MODIFY_BLOG, bPermissionModify );
        model.put( MARK_PERMISSION_DELETE_BLOG, bPermissionDelete );
        model.put( MARK_PERMISSION_PUBLISH_BLOG, bPermissionPublish );

        return getPage( PROPERTY_PAGE_TITLE_MANAGE_BLOG, TEMPLATE_MANAGE_BLOGS, model );
    }
    /**
     * Return View history page
     * @param request The request
     * @return the hostory page
     */
    @View( value = VIEW_HISTORY_BLOG )
    public String getHistoryBlog( HttpServletRequest request )
    {
    	_blog = null;
        int nId = Integer.parseInt( request.getParameter( PARAMETER_ID_BLOG ) );
        List<Blog> listBlogsVersions = BlogHome.getBlogsVersionsList( nId );

        UrlItem urlHistory = new UrlItem( JSP_MANAGE_BLOGS );
        urlHistory.addParameter( PARAMETER_VIEW, VIEW_HISTORY_BLOG );
        urlHistory.addParameter( PARAMETER_ID_BLOG, nId );

        Map<String, Object> model = getPaginatedListModel( request, MARK_BLOG_LIST, listBlogsVersions, urlHistory.getUrl( ) );

        model.put( MARK_ID_BLOG, nId );

        return getPage( PROPERTY_PAGE_TITLE_HISTORY_BLOG, TEMPLATE_HISTORY_BLOG, model );
    }

    /**
     * Returns the form to create a blog
     *
     * @param request
     *            The Http request
     * @return the html code of the blog form
     * @throws AccessDeniedException 
     */
    @View( VIEW_CREATE_BLOG )
    public String getCreateBlog( HttpServletRequest request ) throws AccessDeniedException
    {
    	 if ( !RBACService.isAuthorized( Blog.PROPERTY_RESOURCE_TYPE, RBAC.WILDCARD_RESOURCES_ID,
                 Blog.PERMISSION_CREATE, getUser( ) ) )
    	 {
    		 throw new AccessDeniedException(  );
    	 }
    	_blog = ( _blog != null ) ? _blog : new Blog( );
    	String useCropImage=DatastoreService.getDataValue( PROPERTY_USE_UPLOAD_IMAGE_PLUGIN, "false" );

        Map<String, Object> model = getModel( );
        
        boolean bPermissionCreate = RBACService.isAuthorized( Tag.PROPERTY_RESOURCE_TYPE, RBAC.WILDCARD_RESOURCES_ID,
                Tag.PERMISSION_CREATE, getUser( ) );     

        model.put( MARK_PERMISSION_CREATE_TAG, bPermissionCreate );        
        model.put( MARK_BLOG, _blog );
        model.put( MARK_LIST_TAG, getTageList( ) );
        model.put( MARK_WEBAPP_URL, AppPathService.getBaseUrl( request ) );
        model.put( MARK_USE_UPLOAD_IMAGE_PLUGIN, Boolean.parseBoolean( useCropImage ));


        return getPage( PROPERTY_PAGE_TITLE_CREATE_BLOG, TEMPLATE_CREATE_BLOG, model );
    }

    

    /**
     * Process the data capture form of a new blog
     *
     * @param request
     *            The Http Request
     * @return The Jsp URL of the process result
     */
    @Action( ACTION_CREATE_BLOG )
    public String doCreateBlog( HttpServletRequest request )
    {

    	if ( RBACService.isAuthorized( Blog.PROPERTY_RESOURCE_TYPE, RBAC.WILDCARD_RESOURCES_ID,
                Blog.PERMISSION_CREATE, getUser( ) ) )
   	 {
			    	 String strAction =  request.getParameter( PARAMETER_ACTION_BUTTON );
			    	_blog.setCreationDate( getSqlDate( ) );
			    	_blog.setUpdateDate( getSqlDate( ) );
			    	_blog.setUser( AdminUserService.getAdminUser( request ).getFirstName( ) );
			    	_blog.setUserCreator( AdminUserService.getAdminUser( request ).getFirstName( ) );
			    	_blog.setVersion( 1 );
			    	_blog.setAttachedPortletId( 0 );
			        populate( _blog, request );
			
			        // Check constraints
			        if ( !validateBean( _blog, VALIDATION_ATTRIBUTES_PREFIX ) )
			        {
			            return redirectView( request, VIEW_CREATE_BLOG );
			        }
			
			        // BlogHome.addInitialVersion( _blog );
			        //MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
			        //DocContent docContent = setContent( multipartRequest, request.getLocale( ) );
			        BlogService.getInstance( ).createDocument( _blog, _blog.getDocContent( ) );
			        
			        if( strAction != null && strAction.equals(PARAMETER_APPLY)){
			        	
			        	return redirect( request, VIEW_MODIFY_BLOG, PARAMETER_ID_BLOG, _blog.getId( ) );
			        }
			
			        addInfo( INFO_BLOG_CREATED, getLocale( ) );
   	 }
        return redirectView( request, VIEW_MANAGE_BLOGS );
    }
    /**
     * Return Json if the tag is added
     * @param request The request
     * @return Json The Json success or echec
     */
    @Action( ACTION_ADD_TAG )
    public String doAddTag( HttpServletRequest request )
    {
        String strIdTag = request.getParameter( PARAMETER_TAG );

    	if ( RBACService.isAuthorized( Tag.PROPERTY_RESOURCE_TYPE, strIdTag,
                Tag.PERMISSION_CREATE, getUser( ) ) )
   	 {
	
        String strTagName = request.getParameter( PARAMETER_TAG_NAME );

        Tag tag = new Tag( Integer.parseInt( strIdTag ), _blog.getTag( ).size( ) + 1 );
        tag.setName( strTagName );

        _blog.addTag( tag );
        
        return JsonUtil.buildJsonResponse( new JsonResponse( "SUCESS" ) );
   	 }
    	return JsonUtil.buildJsonResponse( new JsonResponse( "ECHEC" ) );  

    }
    /**
     * Return Json if the tag is removed
     * @param request
     * @return Json The Json success or echec
     */
    @Action( ACTION_REMOVE_TAG )
    public String doRemoveTag( HttpServletRequest request )
    {
        String strIdTag = request.getParameter( PARAMETER_TAG );
        if ( RBACService.isAuthorized( Tag.PROPERTY_RESOURCE_TYPE, strIdTag,
                Tag.PERMISSION_DELETE, getUser( ) ) )
   	 {
        Tag tag = new Tag( );
        tag.setIdTag( Integer.parseInt( strIdTag ) );
        _blog.deleteTag( tag );

        return JsonUtil.buildJsonResponse( new JsonResponse( "SUCESS" ) );
   	 }
        return JsonUtil.buildJsonResponse( new JsonResponse( "ECHEC" ) ); 

    }
    /**
    * Return Json if the tag is updated
    * @param request
    * @return Json The Json success or echec
    */
    @Action( ACTION_UPDATE_PRIORITY_TAG )
    public String doUpdatePriorityTag( HttpServletRequest request )
    {
        Tag tg = null;
        Tag tagMove = null;
        int nPriorityToSet = 0;
        int nPriority = 0;

        String strIdTag = request.getParameter( PARAMETER_TAG );
        String strAction = request.getParameter( PARAMETER_TAG_ACTION );

        for ( Tag tag : _blog.getTag( ) )
        {
            if ( tag.getIdTag( ) == Integer.parseInt( strIdTag ) )
            {
                tg = tag;
                nPriorityToSet = tag.getPriority( );
                nPriority = tag.getPriority( );
            }
        }
        for ( Tag tag : _blog.getTag( ) )
        {
            if ( strAction.equals( "moveUp" ) && tag.getPriority( ) == nPriority - 1 )
            {
                tagMove = tag;
                tagMove.setPriority( tagMove.getPriority( ) + 1 );
                nPriorityToSet = nPriority - 1;

            }
            else
                if ( strAction.equals( "moveDown" ) && tag.getPriority( ) == nPriority + 1 )
                {
                    tagMove = tag;
                    tagMove.setPriority( tagMove.getPriority( ) - 1 );
                    nPriorityToSet = nPriority + 1;

                }
        }
        tg.setPriority( nPriorityToSet );

        if ( tagMove != null )
        {

            return JsonUtil.buildJsonResponse( new JsonResponse( String.valueOf( tagMove.getIdTag( ) ) ) );

        }
        return JsonUtil.buildJsonResponse( new JsonResponse( String.valueOf( tg.getIdTag( ) ) ) );

    }

    /**
     * Manages the removal form of a blog whose identifier is in the http request
     *
     * @param request
     *            The Http request
     * @return the html code to confirm
     * @throws AccessDeniedException 
     */
    @Action( ACTION_CONFIRM_REMOVE_BLOG )
    public String getConfirmRemoveBlog( HttpServletRequest request ) throws AccessDeniedException
    {
    	 
    	String strId= request.getParameter( PARAMETER_ID_BLOG );
        
        if ( !RBACService.isAuthorized( Blog.PROPERTY_RESOURCE_TYPE, strId,
                Blog.PERMISSION_DELETE, getUser( ) ) )
        {
   		 throw new AccessDeniedException(  );
        }
        
    	int nId = Integer.parseInt( strId );
        UrlItem url = new UrlItem( getActionUrl( ACTION_REMOVE_BLOG ) );
        url.addParameter( PARAMETER_ID_BLOG, nId );

        String strMessageUrl = AdminMessageService.getMessageUrl( request, MESSAGE_CONFIRM_REMOVE_BLOG, url.getUrl( ), AdminMessage.TYPE_CONFIRMATION );

        return redirect( request, strMessageUrl );
    }

    /**
     * Handles the removal form of a blog
     *
     * @param request
     *            The Http request
     * @return the jsp URL to display the form to manage blog
     */
    @Action( ACTION_REMOVE_BLOG )
    public String doRemoveBlog( HttpServletRequest request )
    {
        String strId = request.getParameter( PARAMETER_ID_BLOG ) ;

        int nId = Integer.parseInt( strId );
        
        if ( !RBACService.isAuthorized( Blog.PROPERTY_RESOURCE_TYPE, strId,
                Blog.PERMISSION_DELETE, getUser( ) ) )
        {
	        List<BlogPublication> docPublication = BlogPublicationHome.getDocPublicationByIdDoc( nId );
	
	        if ( docPublication.size( ) > 0 )
	        {
	            String strMessageUrl = AdminMessageService.getMessageUrl( request, MESSAGE_ERROR_DOCUMENT_IS_PUBLISHED, AdminMessage.TYPE_STOP );
	
	            return redirect( request, strMessageUrl );
	        }
	        BlogService.getInstance( ).deleteDocument( nId );
	        ExtendableResourceRemovalListenerService.doRemoveResourceExtentions( Blog.PROPERTY_RESOURCE_TYPE, String.valueOf( nId ) );
	
	        addInfo( INFO_BLOG_REMOVED, getLocale( ) );
        }
        return redirectView( request, VIEW_MANAGE_BLOGS );
    }

    /**
     * Returns the form to update info about a blog
     *
     * @param request
     *            The Http request
     * @return The HTML form to update info
     * @throws AccessDeniedException 
     */
    @View( VIEW_MODIFY_BLOG )
    public String getModifyBlog( HttpServletRequest request ) throws AccessDeniedException
    {
        String strId =  request.getParameter( PARAMETER_ID_BLOG ) ;

        if ( !RBACService.isAuthorized( Blog.PROPERTY_RESOURCE_TYPE, strId,
                Blog.PERMISSION_MODIFY, getUser( ) ) )
        {
   		 throw new AccessDeniedException(  );
        }
        int nId = Integer.parseInt( request.getParameter( PARAMETER_ID_BLOG ) );

        String strResetVersion = request.getParameter( PARAMETER_VERSION_BLOG );
    	String useCropImage=DatastoreService.getDataValue( PROPERTY_USE_UPLOAD_IMAGE_PLUGIN, "false" );

        int nVersion = -1;
        if ( strResetVersion != null )
        {
            nVersion = Integer.parseInt( strResetVersion );
        }

        if ( _blog == null || ( _blog.getId( ) != nId ) || ( strResetVersion != null && _blog.getVersion( ) != nVersion ) )
        {
            if ( strResetVersion != null )
            {
            	_blog = BlogHome.findVersion( nId, nVersion );
            }
            else
            {
            	_blog = BlogService.getInstance( ).loadDocument( nId );
            }
            // _blog.setEditComment("");
        }

        Map<String, Object> model = getModel( );
        
        boolean bPermissionCreate = RBACService.isAuthorized( Tag.PROPERTY_RESOURCE_TYPE, RBAC.WILDCARD_RESOURCES_ID,
                Tag.PERMISSION_CREATE, getUser( ) );

        model.put( MARK_PERMISSION_CREATE_TAG, bPermissionCreate );        
        model.put( MARK_BLOG, _blog );
        model.put( MARK_LIST_TAG, getTageList( ) );
        model.put( MARK_USE_UPLOAD_IMAGE_PLUGIN, Boolean.parseBoolean( useCropImage ));
        model.put( MARK_WEBAPP_URL, AppPathService.getBaseUrl( request ) );

        ExtendableResourcePluginActionManager.fillModel( request, getUser( ), model, String.valueOf( nId ), Blog.PROPERTY_RESOURCE_TYPE );

        return getPage( PROPERTY_PAGE_TITLE_MODIFY_BLOG, TEMPLATE_MODIFY_BLOG, model );
    }

    /**
     * Process the change form of a blog
     *
     * @param request
     *            The Http request
     * @return The Jsp URL of the process result
     */
    @Action( ACTION_MODIFY_BLOG )
    public String doModifyBlog( HttpServletRequest request )
    {
   	 
    	String strId= request.getParameter( PARAMETER_ID_BLOG );
    	String strAction =  request.getParameter( PARAMETER_ACTION_BUTTON );
        int nId = Integer.parseInt( strId );
        String strHtmlContent = request.getParameter( PARAMETER_HTML_CONTENT );
        String strEditComment = request.getParameter( PARAMETER_EDIT_COMMENT );
        String strContentLabel = request.getParameter( PARAMETER_CONTENT_LABEL );
        String strDescription = request.getParameter( PARAMETER_DESCRIPTION );
        String strShareable = request.getParameter( PARAMETER_SHAREABLE );
        String strUrl = request.getParameter( PARAMETER_URL );
        
        if ( RBACService.isAuthorized( Blog.PROPERTY_RESOURCE_TYPE, strId,
                Blog.PERMISSION_MODIFY, getUser( ) ) )
        {
	        Blog latestVersionBlog = BlogHome.findByPrimaryKey( nId );
	        if ( _blog == null || ( _blog.getId( ) != nId ) )
	        {
	        	_blog = latestVersionBlog;
	        }
	
	        _blog.setContentLabel( strContentLabel );
	        _blog.setDescription( strDescription );
	        _blog.setShareable( Boolean.parseBoolean( strShareable ) );
	        _blog.setHtmlContent( strHtmlContent );
	        _blog.setEditComment( strEditComment );
	        _blog.setUpdateDate( getSqlDate( ) );
	        _blog.setUser( AdminUserService.getAdminUser( request ).getFirstName( ) );
	        _blog.setUrl( strUrl );
	
	
	        // Check constraints
	        if ( !validateBean( _blog, VALIDATION_ATTRIBUTES_PREFIX ) )
	        {
	            return redirect( request, VIEW_MODIFY_BLOG, PARAMETER_ID_BLOG, _blog.getId( ) );
	        }
	
	        if( strAction != null && strAction.equals(PARAMETER_APPLY)){
	        	
	        	BlogService.getInstance( ).updateBlogWithoutVersion( _blog, _blog.getDocContent( ) );
	        	return redirect( request, VIEW_MODIFY_BLOG, PARAMETER_ID_BLOG, _blog.getId( ) );
	        
	        }else{
	        	
	        	_blog.setVersion( latestVersionBlog.getVersion( ) + 1 );
	        	BlogService.getInstance( ).updateDocument( _blog, _blog.getDocContent( ) );
	        	addInfo( INFO_BLOG_UPDATED, getLocale( ) );
	        }
        }
        return redirectView( request, VIEW_MANAGE_BLOGS );
    }

    /**
     * Returns the preview of an blog
     *
     * @param request
     *            The Http request
     * @return The HTML form to update info
     */
    @View( VIEW_PREVIEW_BLOG )
    public String getPreviewBlog( HttpServletRequest request )
    {
        int nId = Integer.parseInt( request.getParameter( PARAMETER_ID_BLOG ) );
        String strVersion = request.getParameter( PARAMETER_VERSION_BLOG );
        int nVersion = -1;
        if ( strVersion != null )
        {
            nVersion = Integer.parseInt( strVersion );
        }

        Blog blog;
        if ( strVersion != null )
        {
            blog = BlogHome.findVersion( nId, nVersion );
        }
        else
        {
            blog = BlogService.getInstance( ).loadDocument( nId );
        }
        blog.setBlogPubilcation( BlogPublicationHome.getDocPublicationByIdDoc( nId ) );

        Map<String, Object> model = getModel( );
        model.put( MARK_LIST_TAG, TagHome.getTagsReferenceList( ) );

        model.put( MARK_BLOG, blog );

        ExtendableResourcePluginActionManager.fillModel( request, getUser( ), model, String.valueOf( nId ), Blog.PROPERTY_RESOURCE_TYPE );

        return getPage( PROPERTY_PAGE_TITLE_PREVIEW_BLOG, TEMPLATE_PREVIEW_BLOG, model );
    }

    /**
     * Returns the diff of two blog versions
     *
     * @param request
     *            The Http request
     * @return The HTML form to update info
     */
    @View( VIEW_DIFF_BLOG )
    public String getDiffBlog( HttpServletRequest request )
    {
        int nId = Integer.parseInt( request.getParameter( PARAMETER_ID_BLOG ) );
        String strVersion = request.getParameter( PARAMETER_VERSION_BLOG );
        int nVersion = -1;
        if ( strVersion != null )
        {
            nVersion = Integer.parseInt( strVersion );
        }
        String strVersion2 = request.getParameter( PARAMETER_VERSION_BLOG2 );

        Blog blog;
        if ( strVersion != null )
        {
        	blog = BlogHome.findVersion( nId, nVersion );
        }
        else
        {
        	blog = BlogHome.findByPrimaryKey( nId );
        }

        int nVersion2 = blog.getVersion( ) - 1;
        if ( strVersion2 != null )
        {
            nVersion2 = Integer.parseInt( strVersion2 );
        }

        Blog blog2 = BlogHome.findVersion( nId, nVersion2 );
        if ( blog2 == null )
        {
        	blog2 = BlogHome.findByPrimaryKey( nId );
        }

        if ( blog2.getVersion( ) > blog.getVersion( ) )
        {
            Blog tmp = blog2;
            blog2 = blog;
            blog = tmp;
        }

        String strDiff = null;
        HtmlCleaner cleaner = new HtmlCleaner( );
        try
        {
            SAXTransformerFactory tf = (SAXTransformerFactory) TransformerFactory.newInstance( );
            TransformerHandler result = tf.newTransformerHandler( );
            result.getTransformer( ).setOutputProperty( OutputKeys.OMIT_XML_DECLARATION, "yes" );
            StringWriter resultWriter = new StringWriter( );
            result.setResult( new StreamResult( resultWriter ) );
            Locale locale = getLocale( );

            DomTreeBuilder oldHandler = new DomTreeBuilder( );
            cleaner.cleanAndParse( new InputSource( new ByteArrayInputStream( blog2.getHtmlContent( ).getBytes( "UTF-8" ) ) ), oldHandler );
            TextNodeComparator leftComparator = new TextNodeComparator( oldHandler, locale );

            DomTreeBuilder newHandler = new DomTreeBuilder( );
            cleaner.cleanAndParse( new InputSource( new ByteArrayInputStream( blog.getHtmlContent( ).getBytes( "UTF-8" ) ) ), newHandler );
            TextNodeComparator rightComparator = new TextNodeComparator( newHandler, locale );

            HtmlSaxDiffOutput output = new HtmlSaxDiffOutput( result, "" );
            HTMLDiffer differ = new HTMLDiffer( output );
            differ.diff( leftComparator, rightComparator );

            strDiff = resultWriter.toString( );
        }
        catch( Exception e )
        {
            AppLogService.error( "Error generating daisy diff for blog " + nId + ":" + blog.getContentLabel( ) + "; versions (" + blog.getVersion( )
                    + "," + blog2.getVersion( ) + ")", e );
        }

        List<Blog> listBlogsVersions = BlogHome.getBlogsVersionsList( nId );

        Map<String, Object> model = getModel( );
        model.put( MARK_BLOG, blog );
        model.put( MARK_BLOG2, blog2 );
        model.put( MARK_DIFF, strDiff );
        model.put( MARK_BLOG_VERSION_LIST, listBlogsVersions );

        return getPage( PROPERTY_PAGE_TITLE_DIFF_BLOG, TEMPLATE_DIFF_BLOG, model );
    }
    /**

     * Added docContent to the htmlDoc content list
     * @param request The Http request
     * @param htmldoc The HtmlDoc
     */
    @Action( ACTION_ADD_FILE_CONTENT )
    public String addContent( HttpServletRequest request){
    	
    	String base64ImageString= request.getParameter( "fileContent" );  
    	String strFileName= request.getParameter( "fileName" );    	 
    	Date currentTime = new Date();
    	strFileName= strFileName + currentTime.getTime( );
    
    	String delims="[,]";
    	String[] parts = base64ImageString.split(delims);
    	String imageString = parts[1];
    	byte[] imageByteArray = Base64.getDecoder().decode(imageString );

    	InputStream is = new ByteArrayInputStream(imageByteArray);

    	//Find out image type
    	String mimeType = null;
    	String fileExtension = null;
    	try {
    	     mimeType = URLConnection.guessContentTypeFromStream(is); //mimeType is something like "image/jpeg"
    	     String delimiter="[/]";
    	     String[] tokens = mimeType.split(delimiter);
    	     fileExtension = tokens[1];
    	 } catch (IOException ioException){
    		 AppLogService.error( ioException.getStackTrace( ), ioException );
    	 }
    	 
    	 DocContent docContent = new DocContent( );
         docContent.setBinaryValue( imageByteArray );
         docContent.setValueContentType( mimeType );
         docContent.setTextValue( strFileName );

         _blog.addConetnt(docContent);
         
         return JsonUtil.buildJsonResponse( new JsonResponse( strFileName ) );
    	
    }
    /**
     * delete docContent in the htmlDoc content list
     * @param request The Http request
     * @param htmldoc The HtmlDoc
     */
    @Action( ACTION_REMOVE_FILE_CONTENT )
    public String removeContent( HttpServletRequest request){
    	
    	String strFileName= request.getParameter( "fileName" );    	 
        _blog.deleteDocContent( strFileName);
        return JsonUtil.buildJsonResponse( new JsonResponse( strFileName ) );
   	
   }
    /**
     * 
     * @return
     */
    private ReferenceList getBlogFilterList( )
    {
        ReferenceList list = new ReferenceList( );
        list.addItem( MARK_BLOG_FILTER_NAME, "Nom" );
        list.addItem( MARK_BLOG_FILTER_DATE, "Date" );
        list.addItem( MARK_BLOG_FILTER_USER, "Utilisateur" );

        return list;
    }


    /**
     * Set content of the blog
     * @param mRequest
     * @param locale
     * @return the content of the blog
     */

    public DocContent setContent( MultipartHttpServletRequest mRequest, Locale locale )
    {

        FileItem fileParameterBinaryValue = mRequest.getFile( "attachment" );
        // boolean bToResize = ( ( strToResize == null ) || strToResize.equals( "" ) ) ? false : true;

        if ( fileParameterBinaryValue != null ) // If the field is a file
        {

            String strContentType = fileParameterBinaryValue.getContentType( );
            byte [ ] bytes = fileParameterBinaryValue.get( );
            String strFileName = fileParameterBinaryValue.getName( );

            if ( !ArrayUtils.isEmpty( bytes ) )
            {

                DocContent docContent = new DocContent( );
                docContent.setBinaryValue( bytes );
                docContent.setValueContentType( strContentType );
                docContent.setTextValue( strFileName );

                return docContent;
            }

        }

        return null;
    }

    /**
     * 
     * @return BlogList
     */
    private ReferenceList getTageList( )
    {

        ReferenceList BlogList = TagHome.getTagsReferenceList( );
        int index = 0;

        for ( ReferenceItem item : TagHome.getTagsReferenceList( ) )
        {
            for ( Tag tg : _blog.getTag( ) )
            {
                if ( item.getCode( ).equals( String.valueOf( tg.getIdTag( ) ) ) )
                {

                	BlogList.remove( index );
                    index--;
                }
            }
            index++;
        }

        return BlogList;
    }
    /**
     * Gets the current
     *
     * @return the current date in sql format
     */
    public java.sql.Timestamp getSqlDate( )
    {
        java.util.Date utilDate = new java.util.Date( );
        java.sql.Timestamp sqlDate = new java.sql.Timestamp( utilDate.getTime( ) );

        return ( sqlDate );
    }
    


}