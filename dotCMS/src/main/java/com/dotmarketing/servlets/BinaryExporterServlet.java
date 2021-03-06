package com.dotmarketing.servlets;

import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.repackage.com.google.common.io.Files;
import com.dotcms.repackage.org.apache.commons.collections.LRUMap;
import com.dotcms.util.DownloadUtil;
import com.dotcms.uuid.shorty.ShortType;
import com.dotcms.uuid.shorty.ShortyId;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.image.reader.SVGImageReaderSpi;
import com.dotmarketing.portlets.contentlet.business.BinaryContentExporter;
import com.dotmarketing.portlets.contentlet.business.BinaryContentExporterException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.util.*;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;

import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.liferay.util.HttpHeaders.CACHE_CONTROL;
import static com.liferay.util.HttpHeaders.EXPIRES;

/**
 *
 * This servlet allows you invoke content exporters over binary fields.
 * With the following URL syntax you are able to invoke an specific content exporter on a piece of content.
 *
 * /contentAsset/{exporter path}/{content identifier}/{binary field - optional}?{byInode=true&}{exporter specific parameters}
 *
 * {exporter path} is the exporter specific path set by the exporter class. I.E. every exporter must implement an interface method call getPathMapping that
 * defines the path of what the exporter is going to be bound. E.G. The com.dotmarketing.portlets.contentlet.business.exporter.ImageResizeFieldExporter binds
 * to the resize-image path so it can be invoked as /contentAsset/resize-image/...
 *
 * {content identifier} is the identifier of the piece of content that wants to be retrieved. Special case occurs when the url parameter "byInode=true" is set
 * then the content specific inode must be passed here.
 *
 * {binary field - optional} is the binary field velocity name (refer to the structure manager to fidn out which is your field velocity name). This url part could be
 * obeyed for certain exporters that operate over the entire content instead of an specific field like with an XML content exporter for example.
 *
 * {exporter specific parameters} is for exporter specific parameters, refer to the exporter documentation. Exporters like the thubmnail generator takes parameters
 * like the width or height of the thumbnail to be generated.
 *
 * @author David Torres 2010
 *
 */
public class BinaryExporterServlet extends HttpServlet {

	private static final FileAssetAPI fileAssetAPI = APILocator.getFileAssetAPI();
	private static final UserAPI userAPI = APILocator.getUserAPI();
	Map<String, BinaryContentExporter> exportersByPathMapping;

	private long defaultLang = APILocator.getLanguageAPI().getDefaultLanguage().getId();

	@SuppressWarnings("unchecked")
	@Override
	public void init() throws ServletException {
		super.init();

		exportersByPathMapping = new HashMap<String, BinaryContentExporter>();

		Iterator<String> keys = Config.getKeys();

		while(keys.hasNext()) {
			String key = keys.next();
			if(key.startsWith("CONTENT_EXPORTERS")) {
				String[]  exporterClasses = Config.getStringArrayProperty(key);
				for(String exporterClassName : exporterClasses) {
					try {
						Class<BinaryContentExporter> exporterClass = (Class<BinaryContentExporter>) Class.forName(exporterClassName);
						BinaryContentExporter exporter = exporterClass.newInstance();
						if(exportersByPathMapping.containsKey(exporter.getPathMapping()))
							Logger.warn(BinaryExporterServlet.class, "There is already an exporter registered to path " + exporter.getPathMapping() +
									" this new exporter: " + exporter.getName() + " will replace the previously registered: " +
									exportersByPathMapping.get(exporter.getPathMapping()).getName());

						Logger.info(this, "Exporter \"" + exporter.getName() + "\" registered for path /" + exporter.getPathMapping());
						exportersByPathMapping.put(exporter.getPathMapping(), exporter);

					} catch (ClassNotFoundException e) {
						Logger.warn(BinaryExporterServlet.class, e.getMessage(), e);
					} catch (InstantiationException e) {
						Logger.warn(BinaryExporterServlet.class, e.getMessage(), e);
					} catch (IllegalAccessException e) {
						Logger.warn(BinaryExporterServlet.class, e.getMessage(), e);
					}
				}
			}
		}
		ImageIO.scanForPlugins();
        final IIORegistry registry = IIORegistry.getDefaultInstance();
        registry.deregisterServiceProvider(registry.getServiceProviderByClass(com.twelvemonkeys.imageio.plugins.svg.SVGImageReaderSpi.class));
        registry.registerServiceProvider(new SVGImageReaderSpi());
	}

	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unchecked")
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String servletPath = req.getServletPath();
		String uri = req.getRequestURI().substring(servletPath.length());
		String[] uriPieces = uri.split("/");
		String exporterPath = uriPieces[1];
		String uuid = uriPieces[2];
		Optional<ShortyId> shortOpt = APILocator.getShortyAPI().getShorty(uuid);
		ShortyId shorty = shortOpt.isPresent() ? shortOpt.get() : APILocator.getShortyAPI().noShorty(uuid);
		boolean isContent= (shorty.subType == ShortType.CONTENTLET);
		uuid = shorty.longId;

		Map<String, String[]> params = new HashMap<String, String[]>();
		params.putAll(req.getParameterMap());
		// only set uri params if they are not set in the query string - meaning
		// the query string will override the uri params.
		Map<String, String[]> uriParams = getURIParams(req);
		for(String x: uriParams.keySet()){
			if(!params.containsKey(x)){
				params.put(x, uriParams.get(x));
			}
		}
		params = sortByKey(params);

		String assetInode = null;
		String assetIdentifier = null;
		boolean byInode = params.containsKey("byInode") || shorty.type == ShortType.INODE ;
		if (byInode){
			assetInode = uuid;
		}
		else{
			assetIdentifier = uuid;
		}

		String fieldVarName = uriPieces.length > 3?uriPieces[3]:"fileAsset";
		BinaryContentExporter exporter = exportersByPathMapping.get(exporterPath);
		if(exporter == null) {
			Logger.warn(this, "No exporter for path " + exporterPath + " is registered. Requested url = " + uri);
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		UserWebAPI userWebAPI = WebAPILocator.getUserWebAPI();
		ContentletAPI contentAPI = APILocator.getContentletAPI();
		BinaryContentExporter.BinaryContentExporterData data = null;
		File inputFile = null;
		HttpSession session = req.getSession(false);
		List<String> tempBinaryImageInodes = null;
        if ( session != null && session.getAttribute( Contentlet.TEMP_BINARY_IMAGE_INODES_LIST ) != null ) {
            tempBinaryImageInodes = (List<String>) session.getAttribute( Contentlet.TEMP_BINARY_IMAGE_INODES_LIST );
        } else {
            tempBinaryImageInodes = new ArrayList<>();
        }

        boolean isTempBinaryImage = tempBinaryImageInodes.contains(assetInode);
        
		ServletOutputStream out = null;
		RandomAccessFile input = null;
		InputStream is = null;
        
		try {
			User user = userWebAPI.getLoggedInUser(req);
			boolean respectFrontendRoles = !userWebAPI.isLoggedToBackend(req);

			//If session is in Admin Mode (Edit Mode) we should respect front end roles also.
			if(session != null && session.getAttribute(com.dotmarketing.util.WebKeys.ADMIN_MODE_SESSION) != null){
				respectFrontendRoles = true;
			}

			String downloadName = "file_asset";
			long lang = WebAPILocator.getLanguageWebAPI().getLanguage(req).getId();


			if (isContent){
				Contentlet content = null;
				if(byInode) {
					if(isTempBinaryImage)
						content = contentAPI.find(assetInode, APILocator.getUserAPI().getSystemUser(), respectFrontendRoles);
					else
						content = contentAPI.find(assetInode, user, respectFrontendRoles);
					assetIdentifier = content.getIdentifier();
				} else {
				    boolean live=userWebAPI.isLoggedToFrontend(req);
				    boolean PREVIEW_MODE = false;
					boolean EDIT_MODE = false;

					if(session != null) {
						PREVIEW_MODE = ((session.getAttribute(com.dotmarketing.util.WebKeys.PREVIEW_MODE_SESSION) != null));
						try {
							EDIT_MODE = (((session.getAttribute(com.dotmarketing.util.WebKeys.EDIT_MODE_SESSION) != null)));
						} catch (Exception e) {
							Logger.error(this, "Error: Unable to determine if there's a logged user.", e);
						}
					}
					//GIT-4506
					if(WebAPILocator.getUserWebAPI().isLoggedToBackend(req)){
						if(!EDIT_MODE && !PREVIEW_MODE)// LIVE_MODE
							live = true;
						else
							live = false;
					}

				    if (req.getSession(false) != null && req.getSession().getAttribute("tm_date")!=null) {
				        live=true;
				        Identifier ident=APILocator.getIdentifierAPI().find(assetIdentifier);
				        if(UtilMethods.isSet(ident.getSysPublishDate()) || UtilMethods.isSet(ident.getSysExpireDate())) {
				            Date fdate=new Date(Long.parseLong((String)req.getSession().getAttribute("tm_date")));
				            if(UtilMethods.isSet(ident.getSysPublishDate()) && ident.getSysPublishDate().before(fdate))
				                live=false;
				            if(UtilMethods.isSet(ident.getSysExpireDate()) && ident.getSysExpireDate().before(fdate))
				                return; // expired!
				        }
				    }

					//If the DEFAULT_FILE_TO_DEFAULT_LANGUAGE is true and the default language is NOT equals to the language we have in request/session...
					if ( Config.getBooleanProperty("DEFAULT_FILE_TO_DEFAULT_LANGUAGE", false)
							&& defaultLang != lang ) {

						ContentletAPI contentletAPI = APILocator.getContentletAPI();

						//Build the lucene query with the identifier and both languages, the default and one in session to see what we can find
						StringBuilder query = new StringBuilder();
						query.append("+(languageId:").append(defaultLang).append(" languageId:").append(lang).append(") ");
						query.append("+identifier:").append(assetIdentifier).append(" +deleted:false ");
						if ( live ) {
							query.append("+live:true ");
						} else {
							query.append("+working:true ");
						}

						List<Contentlet> foundContentlets = contentletAPI.search(query.toString(), 2, -1, null, user, respectFrontendRoles);
						if ( foundContentlets != null && !foundContentlets.isEmpty() ) {
							//Prefer the contentlet with the session language
							content = foundContentlets.get(0);
							if ( content.getLanguageId() != lang && foundContentlets.size() == 2 ) {
								content = foundContentlets.get(1);
							}
						} else {
							Logger.error(this, "Content with Identifier [" + assetIdentifier + "] not found.");
							resp.sendError(404);
							return;
						}

					}
					else {
						/*
						If the property DEFAULT_FILE_TO_DEFAULT_LANGUAGE is false OR the language in request/session
						is equals to the default language, continue with the default behavior.
						 */
						content = contentAPI.findContentletByIdentifier(assetIdentifier, live, lang, user, respectFrontendRoles);
					}
					assetInode = content.getInode();
				}

                // If the user is NOT logged in the backend then we cannot show content that is NOT live.
                // Temporal files should be allowed any time
                if(!isTempBinaryImage && !WebAPILocator.getUserWebAPI().isLoggedToBackend(req)) {
                    if (!APILocator.getVersionableAPI().hasLiveVersion(content) && respectFrontendRoles) {
                        Logger.debug(this, "Content " + fieldVarName + " is not publish, with inode: "
                                + content.getInode());
                        resp.sendError(404);
                        return;
                    }
                }

				//Find the contentlet content type
				ContentType type = APILocator.getContentTypeAPI(APILocator.systemUser()).find((content.getContentTypeId()));
				//And the file asset field
				com.dotcms.contenttype.model.field.Field field;

				try {
					field = APILocator.getContentTypeFieldAPI().byContentTypeAndVar(type, fieldVarName);
				} catch (NotFoundInDbException e) {
					Logger.debug(this,"Field " + fieldVarName + " does not exist within structure " + type.variable());
					resp.sendError(404);
					return;
				}

				if(isTempBinaryImage)
					inputFile = contentAPI.getBinaryFile(content.getInode(), field.variable(), APILocator.getUserAPI().getSystemUser());
				else
					inputFile = contentAPI.getBinaryFile(content.getInode(), field.variable(), user);
				if(inputFile == null){
					Logger.debug(this,"binary file '" + fieldVarName + "' does not exist for inode " + content.getInode());
					resp.sendError(404);
					return;
				}
				downloadName = inputFile.getName();
			}

			//DOTCMS-5674
			if(UtilMethods.isSet(fieldVarName)){
				params.put("fieldVarName", new String[]{fieldVarName});
				params.put("assetInodeOrIdentifier", new String[]{uuid});
			}
			data = exporter.exportContent(inputFile, params);

			// THIS IS WHERE THE MAGIC HAPPENS
			// save to session if user looking to edit a file
			if (req.getParameter(WebKeys.IMAGE_TOOL_SAVE_FILES) != null) {
                Map<String, String> files;
                if ( session != null && session.getAttribute( WebKeys.IMAGE_TOOL_SAVE_FILES ) != null ) {
                    files = (Map<String, String>) session.getAttribute( WebKeys.IMAGE_TOOL_SAVE_FILES );
                } else {
                    files = new HashMap<>();
                }
                String ext = UtilMethods.getFileExtension(data.getDataFile().getName());
		    	File tmp = File.createTempFile("binaryexporter", "." +ext);
		    	FileUtil.copyFile(data.getDataFile(), tmp);
		    	tmp.deleteOnExit();
		    	if (req.getParameter("binaryFieldId") != null) {
		    		files.put(req.getParameter("binaryFieldId"), tmp.getCanonicalPath());
		    	} else {
		    		files.put(fieldVarName, tmp.getCanonicalPath());
		    	}
		    	resp.getWriter().println(UtilMethods.encodeURIComponent(PublicEncryptionFactory.encryptString(tmp.getAbsolutePath())));
		    	resp.getWriter().close();
		    	resp.flushBuffer();
		    	return;
			}

			/*******************************
			 *
			 *  Start serving the data
			 *
			 *******************************/
			long _fileLength = data.getDataFile().length();
			
			String mimeType = fileAssetAPI.getMimeType(data.getDataFile().getName());

			if (mimeType == null) {
				mimeType = "application/octet-stream";
			}
			
			resp.setHeader("Content-Disposition", "inline; filename=\"" + UtilMethods.encodeURL(downloadName) + "\"" );
			resp.setHeader("Content-Length", String.valueOf(_fileLength));

			resp.setContentType(mimeType);
			
			if (req.getParameter("dotcms_force_download") != null || req.getParameter("force_download") != null) {

				// if we are downloading a jpeg version of a png or gif
				String x = UtilMethods.getFileExtension(downloadName);
				String y = UtilMethods.getFileExtension(data.getDataFile().getName());
				if(!x.equals(y)){
					downloadName = downloadName.replaceAll("\\." + x, "\\." + y);
				}
				resp.setHeader("Content-Disposition", "attachment; filename=\"" + UtilMethods.encodeURL(downloadName) + "\"");
			
			} else {

				boolean _adminMode = false;
				try {
				    _adminMode = (session!=null && session.getAttribute(com.dotmarketing.util.WebKeys.ADMIN_MODE_SESSION) != null);
				}catch(Exception e){
				    Logger.warn(this, "An unexpected problem happened when trying to retrieve the backend session");
				    Logger.debug(this, "Something happened!", e);
				}

			    // Set the expiration time
				if (!_adminMode) {

				    int _daysCache = 365;
				    GregorianCalendar expiration = new GregorianCalendar();
					expiration.add(java.util.Calendar.DAY_OF_MONTH, _daysCache);
					int seconds = (_daysCache * 24 * 60 * 60);

					long _lastModified = data.getDataFile().lastModified();
					if(_lastModified < 0) {
					    _lastModified = 0;
					}
					// we need to round the _lastmodified to get rid of the milliseconds.
					_lastModified = _lastModified / 1000;
					_lastModified = _lastModified * 1000;
					Date _lastModifiedDate = new java.util.Date(_lastModified);

					
					String _eTag = "dot:" + assetInode + ":" + _lastModified + ":" + _fileLength;

					SimpleDateFormat httpDate = new SimpleDateFormat(Constants.RFC2822_FORMAT);
					httpDate.setTimeZone(TimeZone.getTimeZone("GMT"));
		            /* Setting cache friendly headers */
					if (!resp.containsHeader(EXPIRES)) {
						resp.setHeader(EXPIRES, httpDate.format(expiration.getTime()));
					}

		            if (!resp.containsHeader(CACHE_CONTROL)) {
						resp.setHeader(CACHE_CONTROL, "public, max-age=" + seconds);
					}
		            String ifNoneMatch = req.getHeader("If-None-Match");

		            /*
		             * If the etag matches then the file is the same
		             *
		            */
		            if(ifNoneMatch != null){
		                if(_eTag.equals(ifNoneMatch) || ifNoneMatch.equals("*")){
		                    resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED );
		                    return;
		                }
		            }

		            resp.setHeader("Last-Modified", httpDate.format(_lastModifiedDate));
		            resp.setHeader("Content-Length", String.valueOf(_fileLength));
		            resp.setHeader("ETag", _eTag);

                /* if we are in ADMIN MODE, don't cache */
				}else{
				    GregorianCalendar expiration = new GregorianCalendar();
					expiration.add(java.util.Calendar.MONTH, -1);
					if (!resp.containsHeader(EXPIRES)) {
						resp.setHeader(EXPIRES, DownloadUtil.httpDate.get().format(expiration.getTime()));
					}

					if (!resp.containsHeader(CACHE_CONTROL)) {
						resp.setHeader(CACHE_CONTROL, "max-age=-1");
					}
				}
			}

			String rangeHeader = req.getHeader("range");
			if(UtilMethods.isSet(rangeHeader)){

				try {
					out = resp.getOutputStream();

					byte[] dataBytes = Files.toByteArray(data.getDataFile());

					//extract range header
					 resp.setHeader("Accept-Ranges", "bytes");
					// Range header should match format "bytes=n-n,n-n,n-n...". If not, then return 416.
					if (!rangeHeader.matches("^bytes=\\d*-\\d*(,\\d*-\\d*)*$")) {
						resp.setHeader("Content-Range", "bytes */" + dataBytes.length); // Required in 416.
						resp.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
						return;
					}
					//parse multiple range bytes
					ArrayList<SpeedyAssetServletUtil.ByteRange> ranges = SpeedyAssetServletUtil.parseRange(rangeHeader, dataBytes.length);
					if (ranges != null){
						SpeedyAssetServletUtil.ByteRange full = new SpeedyAssetServletUtil.ByteRange(0, data.getDataFile().length() - 1, data.getDataFile().length());
						if (ranges.isEmpty() || ranges.get(0).equals(full)) {
							// Return full file.
							input = new RandomAccessFile(data.getDataFile(), "r");
							SpeedyAssetServletUtil.ByteRange r = full;
							resp.setContentType(fileAssetAPI.getMimeType(data.getDataFile().getName()));
							resp.setHeader("Content-Range", "bytes " + r.start + "-" + r.end + "/" + r.total);
							resp.setHeader("Content-Length", String.valueOf(r.length));
							// Copy full range.
							SpeedyAssetServletUtil.copy(input, out, r.start, r.length);
						} else if (ranges.size() == 1){
							SpeedyAssetServletUtil.ByteRange range = ranges.get(0);
							input = new RandomAccessFile(data.getDataFile(), "r");
							// Check if Range is syntactically valid. If not, then return 416.
							if (range.start > range.end) {
								resp.setHeader("Content-Range", "bytes */" + dataBytes.length); // Required in 416.
								resp.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
								return;
							}
							resp.setContentType(fileAssetAPI.getMimeType(data.getDataFile().getName()));
							resp.setHeader("Content-Range", "bytes " + range.start + "-" + range.end + "/" + range.total);
							resp.setHeader("Content-Length", String.valueOf(range.length));
				            resp.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT); // 206.
							SpeedyAssetServletUtil.copy(input, out, range.start, range.length);
						}else{
							resp.setContentType("multipart/byteranges; boundary=" + SpeedyAssetServletUtil.MULTIPART_BOUNDARY);
							resp.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
						    input = new RandomAccessFile(data.getDataFile(), "r");
							for (SpeedyAssetServletUtil.ByteRange r : ranges) {
								if (r.start > r.end) {
									resp.setHeader("Content-Range", "bytes */" + dataBytes.length); // Required in 416.
									resp.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
									return;
								}
								// Add multipart boundary and header fields for every range.
								out.println();
								out.println("--" + SpeedyAssetServletUtil.MULTIPART_BOUNDARY);
								out.println("Content-Type: " + fileAssetAPI.getMimeType(data.getDataFile().getName()));
								out.println("Content-Range: bytes " + r.start + "-" + r.end + "/" + r.total);
								out.println();

								// Copy single part range of multi part range.
								SpeedyAssetServletUtil.copy(input, out, r.start, r.length);
							}
							// End with multipart boundary.
							out.println();
							out.println("--" + SpeedyAssetServletUtil.MULTIPART_BOUNDARY + "--");
						}
					}
				} catch (Exception e) {
					Logger.warn(this, e + " Error for = " + req.getRequestURI() + (req.getQueryString() != null?"?"+req.getQueryString():"") );
					Logger.debug(this, "Error serving asset = " + req.getRequestURI() + (req.getQueryString() != null?"?"+req.getQueryString():""), e);

				}
			}else{
				is = java.nio.file.Files.newInputStream(data.getDataFile().toPath());
	            int count = 0;
	            byte[] buffer = new byte[4096];
	            out = resp.getOutputStream();
	            
	            while((count = is.read(buffer)) > 0) {
	            	out.write(buffer, 0, count);
	            }
	            
			}
            
		} catch (DotContentletStateException e) {
			Logger.debug(BinaryExporterServlet.class, e.getMessage(),e);
            if(!resp.isCommitted()){
              resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
		} catch (DotRuntimeException e) {
			Logger.debug(BinaryExporterServlet.class, e.getMessage(),e);
            if(!resp.isCommitted()){
              resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
		} catch (PortalException e) {
			Logger.error(BinaryExporterServlet.class, e.getMessage());
			Logger.debug(BinaryExporterServlet.class, e.getMessage(),e);
            if(!resp.isCommitted()){
              resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
		} catch (SystemException e) {
			Logger.error(BinaryExporterServlet.class, e.getMessage());
			Logger.debug(BinaryExporterServlet.class, e.getMessage(),e);
            if(!resp.isCommitted()){
              resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
		} catch (DotDataException e) {
			Logger.error(BinaryExporterServlet.class, e.getMessage());
			Logger.debug(BinaryExporterServlet.class, e.getMessage(),e);
	         if(!resp.isCommitted()){
	           resp.sendError(HttpServletResponse.SC_NOT_FOUND);
	         }
		} catch (DotSecurityException e) {
			try {
			  if(req.getSession()!=null){
				if(WebAPILocator.getUserWebAPI().isLoggedToBackend(req)){
				    req.getSession().removeAttribute(com.dotmarketing.util.WebKeys.REDIRECT_AFTER_LOGIN);
					resp.sendError(HttpServletResponse.SC_FORBIDDEN);
				}else{
				    req.getSession().setAttribute(com.dotmarketing.util.WebKeys.REDIRECT_AFTER_LOGIN, req.getAttribute("javax.servlet.forward.request_uri"));
					resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
				}
			  }
			} catch (Exception e1) {
				Logger.error(BinaryExporterServlet.class,e1.getMessage(),e1);
	            if(!resp.isCommitted()){
	              resp.sendError(HttpServletResponse.SC_NOT_FOUND);
	            }
			}
		} catch (BinaryContentExporterException e) {
			Logger.debug(BinaryExporterServlet.class, e.getMessage(),e);
			Logger.error(BinaryExporterServlet.class, e.getMessage());
			if(!resp.isCommitted()){
			  resp.sendError(HttpServletResponse.SC_NOT_FOUND);
			}
		}catch (Exception e) {
			Logger.debug(BinaryExporterServlet.class, e.getMessage(),e);
			Logger.error(BinaryExporterServlet.class, e.getMessage());
            if(!resp.isCommitted()){
              resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
		}
		// close our resources no matter what
		finally{
			
			if(input!=null){
				try{
					input.close();
				}
				catch(Exception e){
					Logger.debug(BinaryExporterServlet.class, e.getMessage());
				}
			}
			
			if(is!=null){
				try{
					is.close();
				}
				catch(Exception e){
					Logger.debug(BinaryExporterServlet.class, e.getMessage());
				}
			}
			
			if(out!=null){
				try{
					out.close();
				}
				catch(Exception e){
					Logger.debug(BinaryExporterServlet.class, e.getMessage());
				}
			}
			
			
		}
		
	}

	@SuppressWarnings("unchecked")
	private Map sortByKey(Map map) {
		List list = new LinkedList(map.entrySet());
		Collections.sort(list, new Comparator() {
			public int compare(Object o1, Object o2) {
				return ((Comparable) ((Map.Entry) (o1)).getKey()).compareTo(((Map.Entry) (o2)).getKey());
			}
		});
		// logger.info(list);
		Map result = new LinkedHashMap();
		for (Iterator it = list.iterator(); it.hasNext();) {
			Map.Entry entry = (Map.Entry) it.next();
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}



	// Tries to find out whether this is content or a file
private boolean isContent(String id, boolean byInode, long langId, boolean respectFrontendRoles) throws DotStateException, DotDataException, DotSecurityException{



		if(cacheMisses.containsKey(id+byInode)){
			throw new DotStateException("404 - Unable to find id:" + id);
		}


		if(byInode){
			try {
				Contentlet c =APILocator.getContentletAPI().find(id, userAPI.getSystemUser(), true);
				if(c != null && c.getInode() != null)
					return true;
			} catch (Exception e) {
				Logger.debug(this.getClass(), "Unable to find contentlet " + id);
			}
		}

		else{
			try {

				//Lest try first to find the identifier in cache
				Identifier identifier = APILocator.getIdentifierAPI().loadFromCache(id);
				if ( identifier != null ) {
					return "contentlet".equals(identifier.getAssetType());
				}

				//If not found in cache trying in the index
				String luceneQuery = "+identifier:" + id;
				List<Contentlet> foundContentlets = APILocator.getContentletAPI().search(luceneQuery, 0, -1, null, userAPI.getSystemUser(), false);
				if ( foundContentlets != null && !foundContentlets.isEmpty() ) {
					return true;
				}

			} catch (Exception e) {
				Logger.debug(this.getClass(), "cant find identifier " + id);
			}
		}
		cacheMisses.put(id+byInode, true);
		throw new DotStateException("404 - Unable to find id:" + id);

	}
	@SuppressWarnings("deprecation")
	private Map cacheMisses = new LRUMap(1000);

	private Map<String,String[]> getURIParams(HttpServletRequest request){
		String url = request.getRequestURI().toString();
		url = (url.startsWith("/")) ? url.substring(1, url.length()) : url;
		String p[] = url.split("/");
		Map<String, String[]> map = new HashMap<String, String[]>();

		String key =null;
		for(String x : p){
			if(key ==null){
				key = x;
			}
			else{
				map.put(key, new String[]{x});
				key = null;
			}
		}

		return map;

	}



}
