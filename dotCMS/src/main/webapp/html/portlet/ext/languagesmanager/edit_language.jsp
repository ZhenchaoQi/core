<%@ page import="java.io.FileNotFoundException" %>
	<%@ include file="/html/portlet/ext/languagesmanager/init.jsp" %>

<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%

com.dotmarketing.portlets.languagesmanager.model.Language language = request.getAttribute(com.dotmarketing.util.WebKeys.LANGUAGE_MANAGER_LANGUAGE) != null ?  (com.dotmarketing.portlets.languagesmanager.model.Language)request.getAttribute(com.dotmarketing.util.WebKeys.LANGUAGE_MANAGER_LANGUAGE) : new com.dotmarketing.portlets.languagesmanager.model.Language(); 

long languageId = language.getId();
String strlanguage = language.getLanguage();
if(languageId == 0 ){

strlanguage = "New Language";

}



%>





<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
<liferay:param name="box_title" value='<%=strlanguage.equals("New Language")? UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "New-Language")): strlanguage%>' />


<script language="Javascript">

function saveLanguage(form) {
	
	
		form.<portlet:namespace />cmd.value = '<%=Constants.SAVE%>';
		form.<portlet:namespace />redirect.value = '<portlet:renderURL><portlet:param name="struts_action" value="/ext/languages_manager/view_languages_manager" /></portlet:renderURL>';
		form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/languages_manager/edit_language" /></portlet:actionURL>';
		submitForm(form);
		
	
	
}

function deleteLanguage(form) {
if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Are-you-sure-you-want-to-delete-this-language")) %>')){
	
	form.<portlet:namespace />cmd.value = '<%=Constants.DELETE%>';
	form.<portlet:namespace />redirect.value = '<portlet:renderURL><portlet:param name="struts_action" value="/ext/languages_manager/view_languages_manager" /></portlet:renderURL>';
	form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/languages_manager/edit_language" /></portlet:actionURL>';
	submitForm(form);
}
}
function cancelEdit(form) {
	self.location = '<portlet:renderURL><portlet:param name="struts_action" value="/ext/languages_manager/view_languages_manager" /></portlet:renderURL>';
}

</script>

<div class="portlet-main">

	<div class="form-horizontal">
	  
	    
	    <html:form action="/ext/languages_manager/edit_language" styleId="fm">
	    <input name="<portlet:namespace /><%= Constants.CMD %>" type="hidden" value="">
	    <input name="<portlet:namespace />redirect" type="hidden" value="">
	    <html:hidden  property="id" value="<%=String.valueOf(languageId)%>" />
	    
	    <%if(languageId > 0){ %>
		    <dl> 
			    <dt><%= LanguageUtil.get(pageContext, "Language-Id") %>:</dt>
			    <dd><%=languageId %> </dd>
			</dl>
			<dl> 
			    <dt><%= LanguageUtil.get(pageContext, "Use") %>:</dt>
			    <dd>http://yoursite.com/?language_id=<%=languageId %></dd>
			</dl>
	    <%} %>
	    <dl>
		    <dt><%= LanguageUtil.get(pageContext, "Language-Code") %>:</dt>
		    <dd><html:text size="30" property="languageCode" maxlength="2" /></dd>
	    </dl>
		<dl>
		    <dt><%= LanguageUtil.get(pageContext, "Country-Code") %>:</dt>
		    <dd><html:text size="30" property="countryCode" maxlength="2"/></dd>
	    </dl>
		<dl>
		    <dt><%= LanguageUtil.get(pageContext, "Language") %>:</dt>
		    <dd><html:text size="30" property="language" /> <%= LanguageUtil.get(pageContext, "descriptive") %></dd>
	    </dl>
		<dl>
		    <dt><%= LanguageUtil.get(pageContext, "Country") %>:</dt>
		    <dd><html:text size="30" property="country" /> <%= LanguageUtil.get(pageContext, "descriptive") %></dd>
		</dl>
	 
	    <div class="buttonRow">
	    	<button dojoType="dijit.form.Button" onClick="saveLanguage(document.getElementById('fm'))" iconClass="saveIcon">
	           <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "save")) %>
	        </button>
	        
			<%if (language.getId() != 0){%>
		        <button dojoType="dijit.form.Button" onClick="deleteLanguage(document.getElementById('fm'))" class="dijitButtonDanger">
		           <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "delete")) %>
		        </button> 
	        <%}%>
	        
	        <button dojoType="dijit.form.Button" onClick="cancelEdit()" class="dijitButtonFlat" >
	            <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "cancel")) %>
	        </button>
	        
	        
	    </div>
	
	    </html:form>
	</div>
</div>
</liferay:box>
