# RSS Importer

Imports Blog Entries from an external RSS feed

## Build instructions

clone into a Liferay DXP 7.2 workspace's `modules` directory.
Tested with target platform 7.2.10.1

## Usage instructions

* No effort has been made to extract feed-specific behavior:
* Most likely needs adaptation to the actual content to be included: This version tries to extract an image and cuts of a well-known "read more" link from the end, which will most likely be different for every other blog.
* Drop portlet on a page, follow instructions on screen. 
* Note: "Delete" operation will delete _all_ of your blog content. You have been warned.

  