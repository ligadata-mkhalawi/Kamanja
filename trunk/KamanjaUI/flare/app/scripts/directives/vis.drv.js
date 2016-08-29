/*globals angular,_,vis, console, document */
'use strict';

angular
  .module('flareApp')
  .directive('vis', ['$rootScope',
    function ($rootScope) {
      return {
        restrict: 'E',
        scope: {},
        template: '<div><div id="visJsDiv"></div>',
        replace: true,
        link: function (scope, element) {

          var network;

          var nodes = [

            {id: 'AppAccessLog', _label: 'AppAccessLog', group: 'logsCluster', color: {border: ''}},
            {id: 'URLAccessLog', _label: 'URLAccessLog', group: 'logsCluster'},
            {id: 'RemoteAccessLog', _label: 'RemoteAccessLog', group: 'logsCluster'},
            {id: 'RootLog', _label: 'RootLog', hidden: true, group: 'logsCluster'},

            {id: 'Mozilla Browser', _label: 'Mozilla Browser', group: 'browsersCluster', color: {border: ''}},
            {id: 'Chrome Browser', _label: 'Chrome Browser', group: 'browsersCluster'},
            {id: 'RootBrowser', _label: 'RootBrowser', hidden: true, group: 'browsersCluster'},

            {id: 'Jane', _label: 'Jane', group: 'userCluster'},
            {id: 'John', _label: 'John', group: 'userCluster'},
            {id: 'Jill', _label: 'Jill', group: 'userCluster'},
            {id: 'RootUser', _label: 'RootUser', hidden: true, group: 'userCluster'},

            {id: 'BadApp', _label: 'BadApp', group: 'BadAppsCluster'},
            {id: 'RootBadApp', _label: 'RootBadApp', hidden: true, group: 'BadAppsCluster'},

            {id: 'Outlook', _label: 'Outlook', group: 'EmailAppsCluster'},
            {id: 'GMail', _label: 'GMail', group: 'EmailAppsCluster'},
            {id: 'RootEmailApp', _label:'RootEmailApp', hidden: true, group: 'EmailAppsCluster'}

          ];
          var edges = [

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

          ];
          _.each(edges, function (edge){
            edge.color = edge.color || {};
            edge.color.opacity = 0.3 ;
            edge.label = '';
          });

          // create a network
          var container = document.getElementById('visJsDiv');
          var data = {
            nodes: nodes,
            edges: edges
          };
          var options = {

            layout: {
              improvedLayout: true
            },

            nodes: {
              shape: 'dot',
              scaling: {
                min: 100,
                max: 300
              },
              font: {
                size: 15,
                // face: 'Tahoma'
              }
            },

            edges: {
              smooth: {
                type: 'dynamic',
                roundness: 0.55,
              },
              arrows:'to',
              font: {
                size: 15,
                background: '#ffffff'
              }
            },

            physics: {
              adaptiveTimestep: true,
              stabilization: false,
              // barnesHut: {
              //   gravitationalConstant: -2000,
              //   centralGravity: 10,
              //   springLength: 50,
              //   avoidOverlap: 0,
              // },
              timestep: 1,
              solver: 'repulsion'
            },

            interaction: {
              hover: true,
              navigationButtons: true,
              keyboard: true,
              hideEdgesOnDrag: true
            },

            groups: {
              'logsCluster': {
                color: {background: '#599465', border: '#599465'},
                font: {color: '#fff'}
              },
              'browsersCluster': {
                color: {background: '#b000cf', border: '#b000cf'}
              },
              'userCluster': {
                color: {background: '#3b6a94', border: '#3b6a94'},
                font: {color: '#fff'}
              },
              'BadAppsCluster': {
                color: {background: '#7fc9c9', border: '#7fc9c9'},
                font: {color: '#fff'}
              },
              'EmailAppsCluster': {
                color: {background: '#d44d48', border: '#d44d48'},
                font: {color: '#fff'}
              }
            }

          };

          network = new vis.Network(container, data, options);

          network.on('afterDrawing', function (ctx) {
            nodes.forEach(function (d) {
              if (d.hidden || !$rootScope.showOptions[$rootScope.currentView]['Labels']) {
                return;
              }
              if (options.groups[d.group].hidden){
                return;
              }
              var position = network.getPositions(d.id)[d.id];
              ctx.textAlign = 'left';
              ctx.font = '9px arial';
              ctx.fillStyle = '#ffffff';
              ctx.fillText(d._label, (position.x + -10), (position.y + -2));
            });
          });

          $rootScope.$on('labelsToggled', function () {
            network.redraw();
          });
          $rootScope.$on('showOptionsToggled', function () {
            var showOptions = $rootScope.showOptions[$rootScope.currentView];
            options.groups['EmailAppsCluster'].hidden = !showOptions['Applications'];
            options.groups['logsCluster'].hidden = !showOptions['Logs'];
            options.groups['userCluster'].hidden = !showOptions['People'];
            options.groups['BadAppsCluster'].hidden = !showOptions['Countries'];
            options.groups['browsersCluster'].hidden = !showOptions['Organizations'];
            _.each(edges,function (edge) {
              var fromAndTo = _.filter(nodes, function(n){
                return n.id === edge.from || n.id === edge.to
              });
              var hidden = false;
              if (options.groups[fromAndTo[0].group].hidden) {
                hidden = true;
              }
              if (options.groups[fromAndTo[1].group].hidden) {
                hidden = true;
              }
              edge.hidden = hidden;
            });
            network.setOptions(options);
            network.setData(data);
          });
        }
      }
    }]);

(function () {
  vis.DataSet.prototype.getItemById = function (id) {
    return this.get({
      filter: function (item) {
        return item.id === id;
      }
    })[0];
  };
  vis.DataSet.prototype.removeAll = function () {
    var self = this;
    this.forEach(function (d) {
      self.remove(d.id);
    });
  };
  vis.DataSet.prototype.updateAll = function (updateObject) {
    'use strict';
    var self = this;
    this.forEach(function (d) {
      updateObject.id = d.id;
      self.update(updateObject);
    });
  };
}());
