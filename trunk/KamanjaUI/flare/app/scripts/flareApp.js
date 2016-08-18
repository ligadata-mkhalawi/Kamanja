/**
 * Created by muhammad on 8/18/16.
 */
'use strict'
angular.module('flareApp', ['ngAnimate', 'ui.router'])
  .config(function ($stateProvider, $urlRouterProvider) {
    $urlRouterProvider.otherwise(function ($injector, $location) {
      var $state = $injector.get('$state');
      if ($location.$$path === '' || $location.$$path === '/') {
        $state.go('/');
      } else
        $state.go('404');
    });

    $stateProvider
      .state('/', {
        templateUrl: 'views/main.html',
        controller: 'mainCtrl as main'
      })
      .state('404', {
        templateUrl: 'views/404.html'
      });
  })
  .controller('mainCtrl',[function(){
    var network;
    var color = 'gray';
    var len = undefined;
    var nodes = [

      {id: "AppAccessLog", label: "AppAccessLog", group: 'logsCluster', color:{border:''}},
      {id: "URLAccessLog", label: "URLAccessLog", group: 'logsCluster'},
      {id: "RemoteAccessLog", label: "RemoteAccessLog", group: 'logsCluster'},
      {id: "RootLog", hidden:true, group: 'logsCluster'},

      {id: "Mozilla Browser", label: "Mozilla Browser", group: 'browsersCluster', color:{border:''}},
      {id: "Chrome Browser", label: "Chrome Browser", group: 'browsersCluster'},
      {id: "RootBrowser", hidden:true, group: 'browsersCluster'},

      {id: "Jane", label: "Jane", group: "userCluster"},
      {id: "John", label: "John", group: "userCluster"},
      {id: "Jill", label: "Jill", group: "userCluster"},
      {id: "RootUser", hidden:true, group: "userCluster"},

      {id: "BadApp", label: "BadApp", group: "BadAppsCluster"},
      {id: "RootBadApp", hidden:true, group: "BadAppsCluster"},

      {id: "Outlook", label: "Outlook", group: "EmailAppsCluster"},
      {id: "GMail", label: "GMail", group: "EmailAppsCluster"},
      {id: "RootEmailApp", hidden:true, group: "EmailAppsCluster"}

    ];

    var edges = [

      // Clustering Node

//        {from: 'Chrome Browser', to: 'RootBrowser', hidden:true, length:150},
//        {from: 'Mozilla Browser', to: 'RootBrowser', hidden:true, length:150},

//        {from: 'AppAccessLog', to: 'RootLog',hidden:true,length:100},
//        {from: 'RemoteAccessLog', to: 'RootLog',hidden:true,length:100},
//        {from: 'URLAccessLog', to: 'RootLog',hidden:true,length:100},

//        {from: 'John', to: 'RootUser', hidden:true, length:100},
//        {from: 'Jane', to: 'RootUser', hidden:true, length:100},
//        {from: 'Jill', to: 'RootUser', hidden:true, length:100},

//        {from: 'Outlook', to: 'RootEmailApp', hidden:true, length:100},
//        {from: 'GMail', to: 'RootEmailApp', hidden:true, length:100},

//        {from: 'BadApp', to: 'RootBadApp', hidden:true, length:100},

      //

      {from: 'Chrome Browser', to: 'AppAccessLog', label:'logsTo'},
      {from: 'Chrome Browser', to: 'URLAccessLog', label:'logsTo'},

      {from: 'Jane', to: 'Chrome Browser', label:'access'},
      {from: 'John', to: 'Chrome Browser', label:'access'},

      {from: 'John', to: 'BadApp', label:'downloads'},
      {from: 'John', to: 'BadApp', label:'installs'},

      {from: 'BadApp', to: 'Outlook', label:'access'},
      {from: 'Jill', to: 'Outlook', label:'access'},

    ]

    // create a network
    var container = document.getElementById('mynetwork');
    var data = {
      nodes: nodes,
      edges: edges
    };
    var options = {
      nodes: {
        shape: 'dot',
        scaling: {
          min: 10,
          max: 30
        },
        font: {
          size: 12,
          face: 'Tahoma'
        }
      },
      edges: {
        width: 2,
        smooth:{
          type:'continuous'
        },
        font:{
          size: 12,
          background: '#ffffff'
        }
      },
      "physics": {
        "barnesHut": {
          "gravitationalConstant": -1000,
          "centralGravity":0.03,
          "springLength": 100,
          "avoidOverlap":0
        }
      },
      groups:{
        'logsCluster':{
          color:{background:'#C0392B',border:'#D1D1D3'},
          font:{color:'#000'},
          // shape:'circle'
        },
        'browsersCluster':{
          color:{background:'#8E44AD',border:'#D1D1D3'},
          // shape:'circle'
        },
        'userCluster':{
          color:{background:'#5DADE2',border:'#D1D1D3'},
          font:{color:'#000'},
          // shape:'circle'
        },
        'BadAppsCluster':{
          color:{background:'#48C9B0',border:'#D1D1D3'},
          font:{color:'#000'},
          // shape:'circle'
        },
        'EmailAppsCluster':{
          color:{background:'#F4D03F',border:'#D1D1D3'},
          font:{color:'#000'},
          // shape:'circle'
        }
      }
    };
    network = new vis.Network(container, data, options);
  }])
;
