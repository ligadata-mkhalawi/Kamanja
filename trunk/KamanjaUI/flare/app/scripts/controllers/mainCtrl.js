angular.module('flareApp')
  .controller('mainCtrl', ['$scope', '$rootScope', 'serviceData', function ($scope, $rootScope, serviceData) {
    var main = this;
    main.showSideBar = true;
    main.removeView = function (view) {
      var index = $rootScope.views.indexOf(view);
      $rootScope.views.splice(index, 1);
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
    main.nodeClicked = function (id) {
      $rootScope.$broadcast('nodeClicked', id);
    };
    serviceData.getGraphData(function (graph) {
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
        var relationshipsOptions = [];
        var keys = _.keys(_.countBy(graph.edges,'_label'));
        relationshipsOptions = _.object(keys,_.map( keys, function (k) {
          return {value:true, showMenu:false};
        }));
        graph.showOptions['Relationships'].options = relationshipsOptions;

        var contextOptions = [];
        var keys = _.filter(_.keys(_.countBy(graph.edges,'context')), function (k) {
          return k !== 'undefined';
        });

        contextOptions = _.object(keys,_.map( keys, function (k) {
          var edge = _.filter(graph.edges, {context: k})[0];
          return {value:true, showMenu:false, edge: edge};
        }));
        graph.showOptions['Contexts'] = {options : contextOptions, value: false};

        graph.showOptions['Entities'] = {'value': true};
        return {
          title: 'new view',
          editTitle: true,
          nodes: graph.nodes,
          edges: graph.edges,
          showOptions: graph.showOptions,
          getContextsCount: function () {
            return _.keys(graph.showOptions['Contexts'].options).length;
          },
          getEdgesCount: function (title) {
            if (!title){
              return this.edges.length;
            }
            return _.filter(this.edges, function (edge) {
              return edge._label === title;
            }).length;
          },
          getEdges: function (title) {
            return _.filter(this.edges, function (edge) {
              return edge._label === title;
            });
          },
          getNodesCount: function (title) {
            if (!title){
              return this.nodes.length;
            }
            return _.filter(this.nodes, function (node) {
              return node.group === title;
            }).length;
          },
          getNodes: function (title) {
            if (!title){
              return this.nodes;
            }
            return _.filter(this.nodes, function (node) {
              return node.group === title;
            });
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
            var hide = true;
            var nodes = this.getNodes(node.group);
            _.each(nodes,function (n){
              if (!n.hidden) {
                hide = false;
              }
            });
            traverse(this.showOptions, node.group, function process(key, obj) {
              obj.value = !hide;
            });
            $rootScope.$broadcast('showOptionsToggled');
          },
          toggleEdge: function (edge) {
            edge.hidden = !edge.hidden;
            $rootScope.$broadcast('showOptionsToggled');
          }

        };
      };
      $rootScope.currentView = createView();
      $rootScope.currentView.editTitle = false;
      $rootScope.currentView.id = 'firstView';
      $rootScope.currentView.title = 'view 1';
      $rootScope.views = [$rootScope.currentView];
      $scope.$on('sideBarToggled', function () {

        main.showSideBar = !main.showSideBar;
      });

      var v = createView();
      v.editTitle = false;
      $rootScope.views.push(v);
    });
  }]);
