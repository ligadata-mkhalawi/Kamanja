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
          var showOptions = {
            'Labels': true
          };
          var nodes = $rootScope.currentView.nodes;
          var edges = $rootScope.currentView.edges;
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
              'Logs': {
                color: {background: '#599465', border: '#599465'},
                font: {color: '#fff'}
              },
              'Organizations': {
                color: {background: '#b000cf', border: '#b000cf'}
              },
              'People': {
                color: {background: '#3b6a94', border: '#3b6a94'},
                font: {color: '#fff'}
              },
              'Countries': {
                color: {background: '#7fc9c9', border: '#7fc9c9'},
                font: {color: '#fff'}
              },
              'Applications': {
                color: {background: '#d44d48', border: '#d44d48'},
                font: {color: '#fff'}
              }
            }
          };

          network = new vis.Network(container, data, options);

          network.on('afterDrawing', function (ctx) {
            nodes.forEach(function (d) {
              if (d.hidden || !showOptions['Labels']) {
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


          $rootScope.$on('showOptionsToggled', function () {
            var view = $rootScope.currentView;
            showOptions['Labels'] = view.getOption('Labels');
            for (var key in options.groups) {
              options.groups[key].hidden = !view.getOption(key);
            }
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
