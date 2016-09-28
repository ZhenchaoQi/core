package com.dotcms.contenttype.business;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.hamcrest.generator.qdox.junit.APITestCase;

import com.dotcms.contenttype.business.sql.ContentTypeSql;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.Expireable;
import com.dotcms.contenttype.model.type.FileAssetContentType;
import com.dotcms.contenttype.model.type.FormContentType;
import com.dotcms.contenttype.model.type.UrlMapable;
import com.dotcms.contenttype.transform.contenttype.DbContentTypeTransformer;
import com.dotcms.contenttype.transform.contenttype.ImplClassContentTypeTransformer;
import com.dotcms.repackage.javax.validation.constraints.NotNull;
import com.dotcms.repackage.org.apache.commons.lang.time.DateUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.util.SQLUtil;
import com.dotmarketing.db.LocalTransaction;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.form.business.FormAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.VelocityUtil;

public class ContentTypeFactoryImpl implements ContentTypeFactory {

	final ContentTypeSql contentTypeSql;
	final ContentTypeCache2 cache;
	
	public ContentTypeFactoryImpl() {
		this.contentTypeSql = ContentTypeSql.getInstance();
		this.cache = CacheLocator.getContentTypeCache2();
	}

	@Override
	public ContentType find(String id) throws DotDataException {
		ContentType type = cache.byInode(id);
		if(type==null){
			type= dbById(id);
			cache.add(type);
		}
		return type;
	}

	@Override
	public ContentType findByVar(final String var) throws DotDataException {
		ContentType type = cache.byVar(var);
		if(type==null){
			type= dbByVar(var);
			cache.add(type);
		}
		return type;

	}

	@Override
	public List<ContentType> findAll() throws DotDataException {
		return dbAll("structuretype,upper(name)");
	}

	@Override
	public void delete(ContentType type) throws DotDataException {
		LocalTransaction.wrapReturn(() -> {
			dbDelete(type);
			cache.remove(type);
			return null;
		});
		
	}

	@Override
	public List<ContentType> findAll(String orderBy) throws DotDataException {
		return dbAll(orderBy);
	}
	
	@Override
	public List<ContentType> findUrlMapped() throws DotDataException {
		return dbSearch(" url_map_pattern is not null ",BaseContentType.ANY.getType(), "mod_date",0,10000);
	}

	@Override
	public List<ContentType> search(String search, int baseType, String orderBy, int offset, int limit) throws DotDataException {
		return dbSearch(search, baseType, orderBy, offset, limit);
	}

	@Override
	public List<ContentType> search(String search, BaseContentType baseType, String orderBy, int offset, int limit)
			throws DotDataException {
		return dbSearch(search, baseType.getType(), orderBy, offset, limit);
	}

	@Override
	public List<ContentType> search(String search, String orderBy, int offset, int limit) throws DotDataException {
		return search(search, 0, orderBy, offset, limit);
	}

	@Override
	public List<ContentType> search(String search, String orderBy) throws DotDataException {
		return search(search, 0, orderBy, 0, Config.getIntProperty("PER_PAGE", 50));
	}

	@Override
	public List<ContentType> search(String search) throws DotDataException {
		return search(search, 0, "mod_date desc", 0, Config.getIntProperty("PER_PAGE", 50));
	}
	
	@Override
	public List<ContentType> search(String search, int limit) throws DotDataException {
		return search(search, 0, "mod_date desc", 0, limit);
	}
	@Override
	public List<ContentType> search(String search, String orderBy, int limit) throws DotDataException {
		return search(search, 0, orderBy, 0, limit);
	}

	@Override
	public int searchCount(String search) throws DotDataException {
		return dbCount(search, 0);
	}

	@Override
	public int searchCount(String search, int baseType) throws DotDataException {
		return dbCount(search, baseType);
	}

	@Override
	public int searchCount(String search, BaseContentType baseType) throws DotDataException {
		return dbCount(search, baseType.getType());
	}

	@Override
	public List<ContentType> findByBaseType(BaseContentType type) throws DotDataException {
		return dbByType(type.getType());
	}
	
	
	@Override
	public ContentType findDefaultType() throws DotDataException {
		return dbSelectDefaultType();
	}
	@Override
	public List<ContentType> findByBaseType(int type) throws DotDataException {
		return dbByType(type);
	}

	@Override
	public ContentType save(ContentType type) throws DotDataException {
	
			return LocalTransaction.wrapReturn(() -> {
				ContentType returnType = dbSaveUpdate(type);
				cache.remove(returnType);
				if(type instanceof UrlMapable) {
				    cache.clearURLMasterPattern();
				}
				return returnType;
			});
		

	}

	@Override
	public ContentType setAsDefault(ContentType type) throws DotDataException{
		if(!type.equals(findDefaultType())){
			LocalTransaction.wrapReturn(() -> {
				ContentType returnType  = dbUpdateDefaultToTrue(type);
				cache.clearCache();
				return returnType;
			});
		}
		
		return type;
	}
	
	
	
	
	
	
	
	
	private ContentType dbSelectDefaultType() throws DotDataException {
		DotConnect dc = new DotConnect()
			.setSQL(this.contentTypeSql.SELECT_DEFAULT_TYPE);
		
		
		return new DbContentTypeTransformer(dc.loadObjectResults()).from();
	}

	private ContentType dbUpdateDefaultToTrue(ContentType type) throws DotDataException {
		
		new DotConnect()
			.setSQL(this.contentTypeSql.UPDATE_ALL_DEFUALT)
			.addParam(false)
			.loadResult();
		type = ContentTypeBuilder.builder(type).defaultType(true).build();
		return save(type);


	}

	private List<ContentType> dbByType(int type) throws DotDataException {
		DotConnect dc = new DotConnect();
		String sql = this.contentTypeSql.SELECT_BY_TYPE;
		dc.setSQL(String.format(sql, "mod_date desc")).addParam(type);

		return new DbContentTypeTransformer(dc.loadObjectResults()).asList();

	}

	private List<ContentType> dbAll(String orderBy) throws DotDataException {
		DotConnect dc = new DotConnect();
		String sql = this.contentTypeSql.SELECT_ALL;
		orderBy = SQLUtil.sanitizeSortBy(orderBy);
		dc.setSQL(String.format(sql, orderBy));

		return new DbContentTypeTransformer(dc.loadObjectResults()).asList();

	}

	private ContentType dbById(@NotNull String id) throws DotDataException {
		DotConnect dc = new DotConnect();
		dc.setSQL(this.contentTypeSql.SELECT_BY_INODE);
		dc.addParam(id);
		List<Map<String, Object>> results;

		results = dc.loadObjectResults();
		if (results.size() == 0) {
			throw new NotFoundInDbException("Content Type with id:'" + id + "' not found");
		}
		return new DbContentTypeTransformer(results.get(0)).from();

	}
	
	@Override
	public String suggestVelocityVar(final String tryVar) throws DotDataException{
		
		DotConnect dc = new DotConnect();
		String var = VelocityUtil.convertToVelocityVariable(tryVar, true);
		for(int i=1;i<100000;i++){
			dc.setSQL(this.contentTypeSql.SELECT_COUNT_VAR);
			dc.addParam(var);
			if(dc.getInt("test")==0){
				return var;
			}
			var = tryVar + String.valueOf(i);
		}
		throw new DotDataException("Unable to suggest a variable name.  Got to:"+ var);
		
	}

	private ContentType dbByVar(String var) throws DotDataException {
		DotConnect dc = new DotConnect();
		dc.setSQL(this.contentTypeSql.SELECT_BY_VAR);
		dc.addParam(var);
		List<Map<String, Object>> results;

		results = dc.loadObjectResults();
		if (results.size() == 0) {
			throw new NotFoundInDbException("Content Type with var:" + var + " not found");
		}
		return new DbContentTypeTransformer(results.get(0)).from();

	}

	private ContentType dbSaveUpdate(final ContentType saveType) throws DotDataException {


		ContentTypeBuilder builder = ContentTypeBuilder.builder(saveType)
				.modDate(DateUtils.round(new Date(), Calendar.SECOND));

		if(saveType.inode()==null){
			builder.inode(UUID.randomUUID().toString()).build();
		}
		
		if(!(saveType instanceof UrlMapable)){
			builder.urlMapPattern(null);
			builder.detailPage(null);
		}
		if(!(saveType instanceof Expireable)){
			builder.expireDateVar(null);
			builder.publishDateVar(null);
		}
		
		
		
		boolean existsInDb = false;
		try{
			dbById(saveType.inode());
			existsInDb=true;
		}
		catch(NotFoundInDbException notThere){
			Logger.debug(getClass(), "structure inode not found in db:" + saveType.inode());
		}
		
		ContentType retType=builder.build();
		
		if (!existsInDb) {
			dbInodeInsert(retType);
			dbInsert(retType);
			// set up default fields;
			List<Field> fields = retType.requiredFields();
			FieldApi fapi = new FieldApiImpl().instance();
			for (Field f : fields) {
				f = FieldBuilder.builder(f).contentTypeId(retType.inode()).build();
				try {
					fapi.save(f, APILocator.systemUser());
				} catch (DotSecurityException e) {
					throw new DotStateException(e);
				}
			}
			return retType;

		} else {

			dbInodeUpdate(retType);
			dbUpdate(retType);
			return new ImplClassContentTypeTransformer(retType).from();
		}
	}

	private void dbInodeUpdate(final ContentType type) throws DotDataException {
		DotConnect dc = new DotConnect();
		dc.setSQL(this.contentTypeSql.UPDATE_TYPE_INODE);
		dc.addParam(type.owner());
		dc.addParam(type.inode());
		dc.loadResult();
	}

	private void dbInodeInsert(final ContentType type) throws DotDataException {
		DotConnect dc = new DotConnect();
		dc.setSQL(this.contentTypeSql.INSERT_TYPE_INODE);
		dc.addParam(type.inode());
		dc.addParam(type.iDate());
		dc.addParam(type.owner());
		dc.loadResult();
	}

	private void dbUpdate(ContentType type) throws DotDataException {
		DotConnect dc = new DotConnect();
		dc.setSQL(this.contentTypeSql.UPDATE_TYPE);
		dc.addParam(type.name());
		dc.addParam(type.description());
		dc.addParam(type.defaultType());
		dc.addParam(type.detailPage());
		dc.addParam(type.baseType().getType());
		dc.addParam(type.system());
		dc.addParam(type.fixed());
		dc.addParam(type.variable());
		dc.addParam(new CleanURLMap(type.urlMapPattern()).toString());
		dc.addParam(type.host());
		dc.addParam(type.folder());
		dc.addParam(type.expireDateVar());
		dc.addParam(type.publishDateVar());
		dc.addParam(type.modDate());
		dc.addParam(type.inode());
		dc.loadResult();
	}

	private void dbInsert(ContentType type) throws DotDataException {
		
		if(ContentTypeApi.reservedStructureNames.contains(type.name().toLowerCase())){
			throw new DotDataException("cannot save a structure with name:" + type.name());
		}
		
		DotConnect dc = new DotConnect();
		dc.setSQL(this.contentTypeSql.INSERT_TYPE);
		dc.addParam(type.inode());
		dc.addParam(type.name());
		dc.addParam(type.description());
		dc.addParam(type.defaultType());
		dc.addParam(type.detailPage());
		dc.addParam(type.baseType().getType());
		dc.addParam(type.system());
		dc.addParam(type.fixed());
		dc.addParam(type.variable());
		dc.addParam(new CleanURLMap(type.urlMapPattern()).toString());
		dc.addParam(type.host());
		dc.addParam(type.folder());
		dc.addParam(type.expireDateVar());
		dc.addParam(type.publishDateVar());
		dc.addParam(type.modDate());
		dc.loadResult();
	}

	private boolean dbDelete(ContentType type) throws DotDataException {

		// default structure can't be deleted
		if (type.defaultType()) {
			throw new DotDataException("contenttype.delete.cannot.delete.default.type");
		}
		if(type.system()){
			throw new DotDataException("contenttype.delete.cannot.delete.system.type");
		}

		// deleting fields
		APILocator.getFieldAPI2().deleteFieldsByContentType(type);

		// make sure folders don't refer to this structure as default fileasset
		// structure
		if (type instanceof FileAssetContentType) {
			updateFolderFileAssetReferences((FileAssetContentType) type);
		}

		deleteContentletsByType(type);
		// remove structure permissions
		APILocator.getPermissionAPI().removePermissions(type);

		// remove structure itself
		DotConnect dc = new DotConnect();
		dc.setSQL(this.contentTypeSql.DELETE_TYPE_BY_INODE).addParam(type.inode()).loadResult();
		dc.setSQL(this.contentTypeSql.DELETE_INODE_BY_INODE).addParam(type.inode()).loadResult();
		return true;
	}

	private List<ContentType> dbSearch(String search, int baseType, String orderBy, int offset, int limit) throws DotDataException {
		int bottom = (baseType == 0) ? 0 : baseType;
		int top = (baseType == 0) ? 100000 : baseType;
		limit = (limit <0) ? 10000:limit;
		// our legacy code passes in raw sql conditions and so we need to detect
		// and handle those
		SearchCondition searchCondition = new SearchCondition(search);
		DotConnect dc = new DotConnect();
		dc.setSQL(String.format(this.contentTypeSql.SELECT_QUERY_CONDITION, searchCondition.condition, SQLUtil.sanitizeSortBy(orderBy)));
		dc.setMaxRows(limit);
		dc.setStartRow(offset);
		dc.addParam(searchCondition.search);
		dc.addParam(searchCondition.search);
		dc.addParam(searchCondition.search);
		dc.addParam(bottom);
		dc.addParam(top);
		
		return new DbContentTypeTransformer(dc.loadObjectResults()).asList();

	}

	private int dbCount(String search, int baseType) throws DotDataException {
		int bottom = (baseType == 0) ? 0 : baseType;
		int top = (baseType == 0) ? 100000 : baseType;

		SearchCondition searchCondition = new SearchCondition(search);

		DotConnect dc = new DotConnect();
		dc.setSQL(String.format(this.contentTypeSql.SELECT_COUNT_CONDITION, searchCondition.condition));
		dc.addParam(searchCondition.search);
		dc.addParam(searchCondition.search);
		dc.addParam(searchCondition.search);
		dc.addParam(bottom);
		dc.addParam(top);
		return dc.getInt("test");
	}

	private void updateFolderFileAssetReferences(FileAssetContentType type) throws DotDataException {
		ContentType defaultFileAssetStructure = findByVar(FileAssetAPI.DEFAULT_FILE_ASSET_STRUCTURE_VELOCITY_VAR_NAME);
		DotConnect dc = new DotConnect();
		dc.setSQL("update folder set default_file_type = ? where default_file_type = ?");
		dc.addParam(defaultFileAssetStructure.inode());
		dc.addParam(type.inode());
		dc.loadResult();
	}


	private void deleteContentletsByType(ContentType type) throws DotDataException {
		// permissions have already been checked at this point
		int limit = 200;
		ContentletAPI conAPI = APILocator.getContentletAPI();
		List<Contentlet> contentlets = new ArrayList<>();
		
		try {
			contentlets = conAPI.findByStructure(type.inode(), APILocator.systemUser(), false, limit, 0);

			while(contentlets.size()>0){
				conAPI.destroy(contentlets, APILocator.systemUser(), false);
				contentlets = conAPI.findByStructure(type.inode(), APILocator.systemUser(), false, limit, 0);
			}
		} catch (DotSecurityException e) {
			throw new DotDataException(e);
		}
		
	}

	/**
	 * parses legacy conditions passed in as raw sql
	 * @author root
	 *
	 */
	 class SearchCondition {
		final String search;
		final String condition;

		 SearchCondition(final String searchOrCondition) {
			if (!UtilMethods.isSet(searchOrCondition) || searchOrCondition.equals("%")) {
				this.condition = "";
				this.search = "%";
			}
			else if (searchOrCondition.contains("<") 
					|| searchOrCondition.contains("=") 
					|| searchOrCondition.contains("<")
					|| searchOrCondition.contains(" like ")
					|| searchOrCondition.contains(" is ")
					) {
				this.search = "%";
				this.condition = (searchOrCondition.toLowerCase().trim().startsWith("and")) ? searchOrCondition : "and " +searchOrCondition;

			} else {
				this.condition = "";
				this.search = "%" + searchOrCondition + "%";

			}
		}

		@Override
		public String toString() {
			return "SearchCondition [search=" + search + ", condition=" + condition + "]";
		}
	}

	 
	class CleanURLMap {
		final String urlMap;
		public CleanURLMap(String url){
			this.urlMap=url;
		}
		
		@Override
		public String toString() {
			String ret=null;
			if(UtilMethods.isSet(urlMap)){
				ret = this.urlMap.trim();
				if(!ret.startsWith("/")){
					ret = "/" + ret;
				}
			}
			return ret;
		}
	}
	
}