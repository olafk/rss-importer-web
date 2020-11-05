# RSS Importer

Imports Blog Entries from an external RSS feed

## Build instructions

* clone into a Liferay workspace's `modules` directory.
* Tested with target platform 7.2.10.1
* Also builds/deploys with `liferay.workspace.product=dxp-7.3-ga1`

## Usage instructions

* Drop portlet on a page
* provide RSS feed link
* choose count larger than 0 (the plugin takes `0` literally!)
* Hit "Import"
 
## Note  

* No effort has been made to extract feed-specific behavior:
* Most likely needs adaptation to the actual content to be included: This version tries to extract an image and cuts of a well-known "read more" link from the end, which will most likely be different for every other blog.
* "Delete" operation will delete **all** of your blog content in this site! (**not** just what has previously been imported) **You have been warned**.
* Content is imported as-is, e.g. images linked from an imported blog article are hotlinked from their old site.
