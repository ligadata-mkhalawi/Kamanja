/*globals angular,_,vis, console, document */
'use strict';

angular
  .module('flareApp')
  .directive('vis', ['$rootScope', '$window',
    function ($rootScope, $window) {
      return {
        restrict: 'E',
        scope: {
          nodeClick: '<'
        },
        template: '<div id="visJsDiv">',
        replace: true,
        link: function (scope, element) {

          var network;
          var showOptions = {
            'Labels': true
          };
          var nodes = $rootScope.currentView.nodes;
          var edges = $rootScope.currentView.edges;
          _.each(edges, function (edge) {
            edge.color = edge.color || {};
            edge.color.opacity = 0.3;
            edge.label = edge._label;
          });

          // create a network
          var container = document.getElementById('visJsDiv');
          var data = {nodes: new vis.DataSet(nodes), edges: new vis.DataSet(edges)};
          var options = {

            layout: {
              improvedLayout: true
            },

            nodes: {
              shape: 'dot',
              scaling: {
                min: 100,
                max: 300
              }
            },

            edges: {
              smooth: {
                type: 'dynamic',
                roundness: 0.55,
              },
              arrows: 'to',
              font: {
                size: 15,
                background: '#ffffff'
              }
            },

            physics: {
              adaptiveTimestep: true,
              stabilization: false,
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
              'Logs': {
                color: {background: '#599465', border: '#599465'},
                font: {color: '#fff'}
              },
              'Browsers': {
                color: {background: '#b000cf', border: '#b000cf'}
              },
              'Users': {
                color: {background: '#3b6a94', border: '#3b6a94'},
                font: {color: '#fff'}
              },
              'BadApps': {
                color: {background: '#7fc9c9', border: '#7fc9c9'},
                font: {color: '#fff'}
              },
              'EmailApps': {
                color: {background: '#d44d48', border: '#d44d48'},
                font: {color: '#fff'}
              },
              'None': {
                color: {background: '#999999', border: '#999999'},
                font: {color: '#fff'}
              }
            }
          };

          network = new vis.Network(container, data, options);

          network.on('click', function (param) {
            scope.nodeClick(param.nodes[0]);
          });
          network.on('afterDrawing', function (ctx) {
            nodes.forEach(function (d) {
              if (d.hidden || !showOptions['Labels']) {
                return;
              }
              if (options.groups[d.group].hidden) {
                return;
              }
              var position = network.getPositions(d.id)[d.id];
              ctx.textAlign = 'left';
              ctx.font = '19px roboto';
              ctx.fillStyle = '#ffffff';
              ctx.fillText(d._label, (position.x + 30), (position.y + -2));
            });
          });


          $rootScope.$on('showOptionsToggled', function () {
            var view = $rootScope.currentView;
            nodes = view.nodes;
            edges = view.edges;

            showOptions['Labels'] = view.getOption('Labels');


            for (var key in options.groups) {
              options.groups[key].hidden = !view.getOption(key);
            }

            _.each(edges, function (edge) {
              var fromAndTo = _.filter(nodes, function (n) {
                return n.id === edge.from || n.id === edge.to
              });
              var hidden;
              if (options.groups[fromAndTo[0].group].hidden ) {
                hidden = true;
              }
              if (options.groups[fromAndTo[1].group].hidden) {
                hidden = true;
              }
              if (fromAndTo[0].hidden || fromAndTo[1].hidden) {
                hidden = true;
              }
              if (hidden !== undefined) {
                edge.hidden = hidden;
              }
              if (! view.showOptions['Relationships']['options'][edge._label].value) {
                edge.hidden = true;
              }
            });
            network.setOptions(options);
            data = {nodes: new vis.DataSet(nodes), edges: new vis.DataSet(edges)};
            network.setData(data);
            network.redraw();
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
