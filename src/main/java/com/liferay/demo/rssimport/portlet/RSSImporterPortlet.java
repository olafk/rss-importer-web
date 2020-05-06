package com.liferay.demo.rssimport.portlet;

import com.liferay.blogs.model.BlogsEntry;
import com.liferay.blogs.service.BlogsEntryLocalService;
import com.liferay.demo.rssimport.constants.RSSImporterPortletKeys;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCPortlet;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextFactory;
import com.liferay.portal.kernel.servlet.taglib.ui.ImageSelector;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.FriendlyURLNormalizerUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.WebKeys;
import com.sun.syndication.feed.synd.SyndCategory;
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.Portlet;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Sections that most likely need to be customized are marked with 
 * <i>CUSTOMIZE</i>
 * 
 * This is a very customer specific importer from an RSS feed to Liferay Blogs.
 * Adapt the code to your own needs. For production bulk import, you might need
 * to temporarily configure the source feed to contain all posts from the blog
 * you'd like to import.
 * 
 * @author Olaf Kock
 */
@Component(immediate = true, 
		   property = { 
				   "com.liferay.portlet.display-category=category.sample",
				   "com.liferay.portlet.header-portlet-css=/css/main.css", 
				   "com.liferay.portlet.instanceable=true",
				   "javax.portlet.display-name=RSSImporter", 
				   "javax.portlet.init-param.template-path=/",
				   "javax.portlet.init-param.view-template=/view.jsp", 
				   "javax.portlet.name=" + RSSImporterPortletKeys.RSSIMPORTER,
				   "javax.portlet.resource-bundle=content.Language",
				   "javax.portlet.security-role-ref=power-user,user"
				   }, 
		   service = Portlet.class)
public class RSSImporterPortlet extends MVCPortlet {

	public void deleteAllBlogEntries(ActionRequest request, ActionResponse response) {
		ThemeDisplay themeDisplay = (ThemeDisplay) request.getAttribute(WebKeys.THEME_DISPLAY);
		long groupId = themeDisplay.getScopeGroupId();
		List<String> results = new ArrayList<String>();

		List<BlogsEntry> blogsEntries = bels.getBlogsEntries(0, 200);
		for (BlogsEntry blogsEntry : blogsEntries) {
			if(blogsEntry.getGroupId() == groupId) {
				bels.deleteBlogsEntry(blogsEntry);
				results.add("deleted " + blogsEntry.getTitle());
			}
		}
		request.setAttribute("entries", results);
	}
	
	
	
	public void importFeed(ActionRequest request, ActionResponse response) throws PortalException {
		ServiceContext serviceContext = ServiceContextFactory.getInstance(request);
		long userId = serviceContext.getUserId();
		long count = ParamUtil.getLong(request, "count");
		String feedURL = ParamUtil.getString(request, "feed");
		
		SyndFeedInput input = new SyndFeedInput();
		SyndFeed feed = null;
		List<String> results = new ArrayList<String>();
		
		try {
			if (feedURL.startsWith("file:///")) {
				feed = input.build(new XmlReader(new URL(feedURL)));
			} else if (feedURL.startsWith("http://") || feedURL.startsWith("https://")) {
				feed = input.build(new XmlReader(openServerConnection(feedURL)));
			} else {
				throw new IllegalArgumentException("Feed URL must start with one of file:///, http:// or https://");
			}

			@SuppressWarnings("unchecked")
			List<SyndEntry> entries = feed.getEntries();
			for (SyndEntry entry : entries) {
				try {
					if (count-- <= 0)
						break;
					String title = entry.getTitle();
					String subtitle = "";
					String coverImageCaption = "";
					String[] trackbacks = new String[] {};
					String imgUrl = null;
					String imgName = null;
					Date publishedDate = entry.getPublishedDate();
					String description = decodeDescription(entry);

					SyndContent contentObject = (SyndContent) entry.getContents().get(0);
					String content = contentObject.getValue();

					// Extract image: If content starts with image, 
					// transform it to the blog's coverImage
					// CUSTOMIZE if you can use a different way to extract an image
					if (content.startsWith("<img")) {
						int imgSrcStart = content.indexOf("src=\"") + 5;
						int imgSrcEnd = content.indexOf("\"", imgSrcStart);
						int imgTagEnd = content.indexOf(">", imgSrcEnd);
						imgUrl = content.substring(imgSrcStart, imgSrcEnd);
						imgName = imgUrl.substring(imgUrl.lastIndexOf("/") + 1);
						content = content.substring(imgTagEnd + 1);
					}
					ImageSelector imageSelector = null;
					if (imgUrl != null) {
						byte[] imageResponse = readFromServer(imgUrl);
						imageSelector = new ImageSelector(imageResponse,
								FriendlyURLNormalizerUtil.normalize("illustration-" + imgName),
								"image/jpeg", "");
					}

					String[] tags = extractTagsFromRSSCategories(entry);
					serviceContext.setAssetTagNames(tags);

					bels.addEntry(userId, 
							title, 
							subtitle, 
							description, 
							content, 
							publishedDate, 
							false, // allowPingbacks
							false, // allowTrackbacks
							trackbacks, 
							coverImageCaption, 
							imageSelector, 
							imageSelector, 
							serviceContext);
					
					results.add(title);
					
				} catch (IllegalArgumentException | IOException | PortalException e) {
					results.add(e.getClass().getName() + " " + e.getMessage() + " for " + entry.getTitle());
				}
			}
		} catch (IOException | IllegalArgumentException | FeedException e) {
			results.add(e.getClass().getName() + " " + e.getMessage());
		}
		request.setAttribute("entries", results);
	}



	private String[] extractTagsFromRSSCategories(SyndEntry entry) {
		@SuppressWarnings("unchecked")
		List<SyndCategory> categories = entry.getCategories();
		ArrayList<String> assetTags = new ArrayList<String>(categories.size());
		for (SyndCategory category : categories) {
			assetTags.add(category.getName());
		}
		String[] tags = new String[assetTags.size()];
		assetTags.toArray(tags);
		return tags;
	}

	private String decodeDescription(SyndEntry entry) {
		String result;
		if(entry.getDescription() != null)
			result = entry.getDescription().getValue();
		else if(entry.getContents().size()>0)
			result = ((SyndContent) entry.getContents().get(0)).getValue();
		else
			result = "";

		// cut a customer-specific "read on" section at the end of the preview
		// CUSTOMIZE if you need similar functionality. There's likely no match
		// with any other import...
		int readOnContent = result.indexOf("<p style=\"margin-top:30px;\" class=\"show-for-medium-up\">");
		if (readOnContent > 0)
			result = result.substring(0, readOnContent);

		return result;
	}

	private byte[] readFromServer(String theUrl) throws IOException {
		InputStream in = openServerConnection(theUrl);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buf = new byte[1024];
		int n = 0;
		while (-1 != (n = in.read(buf))) {
			out.write(buf, 0, n);
		}
		out.close();
		in.close();
		return out.toByteArray();
	}

	// the Java default user-agent often gets denied access, 
	// faking it helps. Stupid "security".
	
	private InputStream openServerConnection(String theUrl) throws MalformedURLException, IOException {
		URL url = new URL(theUrl);
		URLConnection connection = url.openConnection();
		connection.setRequestProperty("User-Agent",
				"Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:75.0) Gecko/20100101 Firefox/75.0");
		return connection.getInputStream();
	}

	@Reference
	BlogsEntryLocalService bels;

}