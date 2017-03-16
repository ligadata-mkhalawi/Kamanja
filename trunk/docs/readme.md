# Modifying LigaData RST/Sphinx documentation

The LigaData RST documentation source can be modified by anyone working in the kamanja repo.  This document tells you how to get started.

## Set up

### Install pip if necessary

```> sudo easy_install pip
> /usr/bin/ruby -e "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)"  # Install homebrew; this takes a few minutes
> brew install python
> brew install pip  # This may not be necessary
> pip install --upgrade pip  # Upgrade pip to latest version
```

For more information about installing pip,
see https://pip.pypa.io/en/stable/installing/#do-i-need-to-install-pip .

### Install Sphinx

You can view and modify the doc source without installing Sphinx but you will not be able to build the docs locally to check your formatting.

To install Sphinx, issue the following command:


```
> pip install sphinx
```

This requires that Python 2.7 be installed on your system.
If you are running CentOS or another system that uses a different version of Python,
you need to have Python 2.7 installed on your system
(but it doesn't have to be running as your main Python version).
You can download all versions of Python from https://www.python.org/downloads/.
If Python 2.7 is not your main Python version,  run the "pip2.7 install sphinx" command instead.
(Caveat lector: we have not confirmed that this works on a system using Python 2.6 as its primary Python version.)

The docs were set up using Sphinx 1.4.9;
the software is revised frequently and you will get a later version with this command.
If you run into problems, you might be able to install an earlier version using a command like "pip install sphinx–1.4.9".

For additional instructions, see http://www.sphinx-doc.org/en/1.4.8/tutorial.html


## Build the docs locally

To build the docs locally:

```
> cd *Kamanja/trunk/docs*
> make html
```

You can view the locally-built HTML files by linking to the file name given by the build
or by going to localhost:8081.

To build the PDF documents locally, do everything above required to build the html docs, plus:

```
> pip install rst2pdf # install the PDF building software.
> make pdf
```
Note that it takes 3-4 minutes to build all the PDFs.
The PDFs are located in the *build/html* subdirectory
or can be accessed by clicking the link in the local HTML build.

You may like the "autohtml" feature.
This spawns a process that keeps running
and automatically rebuilds the HTML docs every time you save a file
rather than waiting for you to run "make html".  To implement this:

- Run the **pip install sphinx-autobuild** command to install the autobuild software.
- Open a browser window for localhost:8081.
- Open a separate terminal window in which to run autohtml; navigate to the trunk/docs directory.
- Run "make autohtml" in this new window.
- Modify a doc file, write the file, and view the changes immediately in your browser window.
  Note that this requires that the Autoload plug-in be implemented in your browser.
  For Firefox, you can download this from:  https://addons.mozilla.org/en-US/firefox/addon/auto-reload/

## RST coding

The RST (ReStructured Text) authoring tools are similar to Markdown;
the content is written in straight text with embedded formatting code.
Sphinx is the tool suite that then builds this source into HTML and PDF.
You can google for documents that detail the RST coding.  A good basic one to get you started is:
http://www.sphinx-doc.org/en/1.5.1/rest.html

Note that, if you are using the list-table directive to create a table,
you need to add a special line to get code within a cell to wrap properly.
So the head of a list-table looks like:

```
.. list-table::
:class: ld-wrap-fixed-table
:widths: 20 20 30 30
:header-rows: 1
```

## Directory structure

The doc source is located in *docs/source* and its subdirectories.

The source is divided by "titles" – so "install-plan" is the Planning and Installation Guide;
"config-ref" is the Configuration File Reference Pages, and so forth.

Files in the doc/source directory hold everything together:

- The *index.rst* file controls the docs "landing page" –
  what is displayed when you click the link to go to the doc set.
  All text is included here as well as a listing of the titles
  and the order in which they are listed in the left frame of the page.
  If you want to add or delete a title from the set, you need to do it here.
- Each title has a .rst file that defines the title to be displayed
  and has a "toctree" that lists the file names (without the .rst suffix)
  to be included in that title.
  If you want to add, delete, or move files within a document,
  you modify the title's .rst file.
- The content of each title is located in a subdirectory to doc/source;
  the subdirectories and the title.rst files use the same name
  so it is easy to make the connections.
- Currently, we have a very flat file structure under docs/source
  but additional subdirectories can be added;
  files at any level can have their own toctree's, and so forth.
- All graphics are stored in separate files under the docs/source/_images directory.
  To include a screen shot or other graphic in the documentation,
  create a separate file for that graphic in the *_images* subdirectory
  then include it in the document using the images directive.
- All styling code is located in the docs/source/_themes/sphinx_rtd_theme directory.


