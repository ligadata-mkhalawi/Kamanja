angular.module('flareApp')
  .controller('mainCtrl', ['$scope', '$rootScope', function ($scope, $rootScope) {
    var main = this;
    main.showSideBar = true;
    main.removeView = function (view) {
      var index = $rootScope.views.indexOf(view);
      $rootScope.views.splice(index,1);
    };
    main.setView = function (view) {
      $rootScope.currentView = view;
      $rootScope.$broadcast('showOptionsToggled');
    };
    main.addNewView = function () {
      var v = createView();
      v.title = 'new view';
      $rootScope.views.push(v);
    };
    main.nodeClicked = function(id){
      $rootScope.$broadcast('nodeClicked', id);
    };

    var createView = function () {
      function traverse(o, option, func) {
        for (var i in o) {
          if (i == option) {
            func.apply(this, [i, o[i]]);
            return;
          }
          if (o[i] !== null && typeof(o[i]) == 'object') {
            //going on step down in the object tree!!
            traverse(o[i], option, func);
          }
        }
      }

      return {
        title : 'new view',
        editTitle: true,
        nodes: [
          {id: 'AppAccessLog', _label: 'AppAccessLog', group: 'Logs', color: {border: ''}},
          {id: 'URLAccessLog', _label: 'URLAccessLog', group: 'Logs'},
          {id: 'RemoteAccessLog', _label: 'RemoteAccessLog', group: 'Logs'},
          {id: 'RootLog', _label: 'RootLog', hidden: true, group: 'Logs'},

          {id: 'Mozilla Browser', _label: 'Mozilla Browser', group: 'Browsers', color: {border: ''}},
          {id: 'Chrome Browser', _label: 'Chrome Browser', group: 'Browsers'},
          {id: 'RootBrowser', _label: 'RootBrowser', hidden: true, group: 'Browsers'},

          {id: 'Jane', _label: 'Jane', group: 'Users'},
          {id: 'John', _label: 'John', group: 'Users'},
          {id: 'Jill', _label: 'Jill', group: 'Users'},
          {id: 'RootUser', _label: 'RootUser', hidden: true, group: 'Users'},

          {id: 'BadApp', _label: 'BadApp', group: 'BadApps'},
          {id: 'RootBadApp', _label: 'RootBadApp', hidden: true, group: 'BadApps'},

          {id: 'Outlook', _label: 'Outlook', group: 'EmailApps'},
          {id: 'GMail', _label: 'GMail', group: 'EmailApps'},
          {id: 'RootEmailApp', _label: 'RootEmailApp', hidden: true, group: 'EmailApps'},

          {id: 'Email', _label: 'Email', group: 'None'},
          {id: 'companyMachin', _label: 'companyMachin', group: 'None'},
          {id: 'ContactList', _label: 'ContactList', group: 'None'},
          {id: 'Website', _label: 'Website', group: 'None'},
          {id: 'SuspiciousWebsiteList', _label: 'SuspiciousWebsiteList', group: 'None'}
        ],
        edges: [

          // {from: 'Chrome Browser', to: 'RootBrowser', hidden: true, length: 150},
          // {from: 'Mozilla Browser', to: 'RootBrowser', hidden: true, length: 150},
          //
          // {from: 'AppAccessLog', to: 'RootLog', hidden: true, length: 100},
          // {from: 'RemoteAccessLog', to: 'RootLog', hidden: true, length: 100},
          // {from: 'URLAccessLog', to: 'RootLog', hidden: true, length: 100},
          //
          // {from: 'John', to: 'RootUser', hidden: true, length: 100},
          // {from: 'Jane', to: 'RootUser', hidden: true, length: 100},
          // {from: 'Jill', to: 'RootUser', hidden: true, length: 100},
          //
          // {from: 'Outlook', to: 'RootEmailApp', hidden: true, length: 100},
          // {from: 'GMail', to: 'RootEmailApp', hidden: true, length: 100},
          //
          // {from: 'BadApp', to: 'RootBadApp', hidden: true, length: 100},

          //

          {from: 'Chrome Browser', to: 'AppAccessLog', label: 'logsTo'},
          {from: 'Chrome Browser', to: 'URLAccessLog', label: 'logsTo'},

          {from: 'Jane', to: 'Chrome Browser', label: 'access'},
          {from: 'John', to: 'Chrome Browser', label: 'access'},

          {from: 'John', to: 'BadApp', label: 'downloads'},
          {from: 'John', to: 'BadApp', label: 'installs'},

          {from: 'BadApp', to: 'Outlook', label: 'access'},
          {from: 'Jill', to: 'Outlook', label: 'access'},

          {from: "Outlook", to: "Email", label: 'sends'},
          {from: "Outlook", to: "ContactList", label: 'access'},
          {from: "Website", to: "SuspiciousWebsiteList", label: 'belongsTo'},
          {from: "BadApp", to: "companyMachin", label: 'savedTo'},
          {from: "Website", to: "Chrome Browser", label: 'notifies'},
          {from: "John", to: "Website", label: 'access'}
        ],
        getNodesCount: function (title) {
          return _.filter(this.nodes, function (node) {
            return node.group === title;
          }).length;
        },
        getNodes: function (title) {
          return _.filter(this.nodes, function (node) {
            return node.group === title;
          });
        },
        showOptions: {
          'Coloring by Cluster': {
            value: true,
            options: {
              'Logs': {value: true, color: 'green',showMenu:false},
              'Users': {value: true, color: 'blue',showMenu:false},
              'Browsers': {value: true, color: 'purple',showMenu:false},
              'Alerts': {value: true, color: 'yellow',showMenu:false},
              'EmailApps': {value: true, color:'red',showMenu:false},
              'BadApps': {value: true, color: 'lightBlue',showMenu:false},
              'None': {value: true, color: 'grey',showMenu:false}
            }
          },
          'Relationships': {value: true},
          'Labels': {value: true}
        },
        getOption: function (option) {
          var valObject;
          traverse(this.showOptions, option, function process(key, obj) {
            valObject = obj;
          });
          return valObject.value;
        },
        getOptions: function (option) {
          var valObject;
          traverse(this.showOptions, option, function process(key, obj) {
            valObject = obj;
          });
          return valObject.options;
        },
        toggleOptions: function (option) {
          var v;
          traverse(this.showOptions, option, function process(key, obj) {
            obj.value = v = !obj.value;
          });
          var nodes = this.getNodes(option);
          _.each(nodes, function (node) {
            node.hidden = !v;
          });
          $rootScope.$broadcast('showOptionsToggled');
        },
        toggleNode: function (node) {
          node.hidden = !node.hidden;
          traverse(this.showOptions, node.group, function process(key, obj) {
            obj.value =  true;
          });
          $rootScope.$broadcast('showOptionsToggled');
        }
      };
    };
    $rootScope.currentView = createView();
    $rootScope.currentView.editTitle = false;
    $rootScope.currentView.id = 'firstView';
    $rootScope.currentView.title = 'Malware Investigation';
    $rootScope.views = [$rootScope.currentView];
    $scope.$on('sideBarToggled', function () {

      main.showSideBar = !main.showSideBar;
    });

    var v = createView();
    v.editTitle = false;
    $rootScope.views.push(v);

  }]);
