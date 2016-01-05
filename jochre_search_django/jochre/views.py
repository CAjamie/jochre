#coding=utf-8
import json
import requests
import paginate
import logging
import os

from django.shortcuts import render
from django.conf import settings

def search(request):
    searchUrl = settings.JOCHRE_SEARCH_URL
    advancedSearch = False
    haveResults = False

    query = ''
    if 'query' in request.GET:
        query = request.GET['query']
    
    author = ''
    if 'author' in request.GET:
        author = request.GET['author']
        
    title = ''
    if 'title' in request.GET:
        title = request.GET['title']
    
    if len(author)>0 or len(title)>0:
        advancedSearch = True

    displayAdvancedSearch = 'none'
    if advancedSearch:
        displayAdvancedSearch = 'visible'
    
    pageNumber = 0
    if 'page' in request.GET:
        pageNumber = int(request.GET['page'])
    
    model = {"query" : query,
             "author" : author,
             "title" : title,
             "displayAdvancedSearch" : displayAdvancedSearch}
             
    if len(query)>0:
        MAX_DOCS=1000
        RESULTS_PER_PAGE=10
        userdata = {"command": "search", "maxDocs": MAX_DOCS, "query": query}
        if len(author)>0:
            userdata['author'] = author
        if len(title)>0:
            userdata['title'] = title
        
        resp = requests.get(searchUrl, userdata)
        
        results = resp.json()
        
        page = paginate.Page(results, page=pageNumber, items_per_page=RESULTS_PER_PAGE)
        
        docIds = ''
        
        for result in page.items:
            if 'volume' in result:
                result['titleAndVolume'] = result['title'] + u", volume " + result['volume']
                if 'titleLang' in result:
                    result['titleLangAndVolume'] = result['titleLang'] + u", באַנד " + result['volume']
                else:
                    result['titleLangAndVolume'] = ""
            else:
                result['titleAndVolume'] = result['title']
                if 'titleLang' in result:
                    result['titleLangAndVolume'] = result['titleLang']
                else:
                    result['titleLangAndVolume'] = ""
            if len(docIds)>0:
                docIds += ','
            docIds += str(result['docId'])
        
        if len(page.items)>0:
            haveResults = True
            userdata = {"command": "snippets", "snippetCount": 8, "snippetSize": 160, "query": query, "docIds": docIds}
            resp = requests.get(searchUrl, userdata)
            model["results"] = page.items
            model["start"] = page.first_item
            model["end"] = page.last_item
            model["resultCount"] = len(results)
            model["pageLinks"] = page.link_map(url="http://localhost:8000?page=$page")
            logging.debug(model["pageLinks"])
            
            snippetMap = resp.json()

            for result in page.items:
                bookId = result['name']
                docId = result['docId']
                snippetObj = snippetMap[str(docId)]
                snippets = snippetObj['snippets']
                snippetsToSend = []
                for snippet in snippets:
                    snippetJson = json.dumps(snippet)
                    userdata = {"command": "textSnippet", "snippet": snippetJson}
                    resp = requests.get(searchUrl, userdata)
                    snippetText = resp.text
                    
                    userdata = {"command": "imageSnippet", "snippet": snippetJson}
                    req = requests.Request(method='GET', url=searchUrl, params=userdata)
                    preparedReq = req.prepare()
                    snippetImageUrl = preparedReq.url
                    backslashPos = snippetImageUrl.find("/", len("https://"), len(snippetImageUrl))
                    snippetImageUrl = snippetImageUrl[backslashPos:len(snippetImageUrl)]
                    
                    pageNumber = snippet['pageIndex']
                    urlPageNumber = pageNumber / 2 * 2;
                    snippetReadUrl = u"https://archive.org/stream/" + bookId + u"#page/n" + str(urlPageNumber) + u"/mode/2up";
                    
                    snippetDict = {"snippetText" : snippetText,
                                   "readOnlineUrl" : snippetReadUrl,
                                   "imageUrl": snippetImageUrl,
                                   "pageNumber": pageNumber }
                    
                    snippetsToSend.append(snippetDict)
                    result['snippets'] = snippetsToSend
                    
    model["haveResults"] = haveResults
    return render(request, 'search.html', model)
