<%@ include file="init.jsp" %>

<portlet:actionURL var="importFeed" name="importFeed"/>
<portlet:actionURL var="deleteAllBlogEntries" name="deleteAllBlogEntries"/>

<aui:form action="<%= importFeed %>" method="post" name="fm" >
	<aui:input label="Feed URL" name="feed" type="text"/>
	<aui:input type="text" label="count" name="count" value="0"/>
	<aui:button type="submit" label="display" value="import"/>

</aui:form>
<hr/>

<liferay-ui:message key="delete-all-blog-entries"/>
<aui:form action="<%=deleteAllBlogEntries %>" method="post" name="fmdel">
	<aui:button type="submit" label="delete" name="delete" value="delete"/>
</aui:form>


<ul>
<%
	List<String> entries = (List<String>) request.getAttribute("entries");
	if(entries != null) {
		for(String entry: entries) {
			out.write("  <li>");
			out.write(entry);
			out.write("</li>\n");
		}
	}

%>
</ul>