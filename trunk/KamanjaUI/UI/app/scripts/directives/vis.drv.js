/*globals angular,_,vis, console, document */
'use strict';

angular
  .module('networkApp')
  .directive('uiHomeVisDemo', ['serviceConfig', 'serviceSocket', '$timeout', '$window', '$rootScope',
    function (serviceConfig, serviceSocket, $timeout, $window, $rootScope) {
      return {
        restrict: 'E',
        scope: {
          showStatus: '@',
          data: '<',
          symbolClasses: '<',
          viewName: '@',
          nodeClick: '<',
          nodeDoubleClick: '<',
          edgeClick: '<',
          groundClick: '<'
        },
        template: '<div><div id="visJsDiv"></div>',
        replace: true,
        link: function (scope, element) {
          var selectedNodes = new Set();
          var isNodeActive, updateNodesImagesToBeInactive, updateNodeToBeActive, updateNodeToBeInactive;
          var container = document.getElementById('visJsDiv');
          var data = {nodes: new vis.DataSet([]), edges: new vis.DataSet([])};
          var options = {
            'edges': {
              'smooth': {
                'type': 'curvedCCW',
                'roundness': 0.35
              },
              shadow: {
                enabled: true,
                color: 'rgba(0,0,0,1)',
                size: 15,
                x: 0,
                y: 0
              },
              arrows: {
                from: {enabled: false, scaleFactor: 0.5}
              }
            },

            'physics': {
              'barnesHut': {
                'gravitationalConstant': -10000,
                'centralGravity': 0.5,
                'springLength': 75,
                'springConstant': 0.09,
                'damping': 0.5,
                'avoidOverlap': 1
              },
              'minVelocity': 2.5,
              'solver': 'barnesHut',
              // 'timestep': 0.79
            },
            // 'physics': {
            //   'forceAtlas2Based': {
            //     'centralGravity': 0.005,
            //     'springLength': 60,
            //     'springConstant': 0.09,
            //     'gravitationalConstant': -80,
            //     'damping': 0.88,
            //     'avoidOverlap': 0.53
            //   },
            //   'minVelocity': 0.6,
            //   'solver': 'forceAtlas2Based',
            //   'timestep': 0.79
            // },
            layout: {
              randomSeed: 2
            },
            nodes: {
              shape: 'dot',
              size: 30,
              font: {
                size: 32
              },
              borderWidth: 0,
              shadow: true
            },
            interaction: {
              hover: true,
              navigationButtons: true,
              keyboard: true
            }
          };

          var network = new vis.Network(container, data, options);

          var Node = function (n) {
            var imagePath = serviceConfig.classImageColorPath;
            var types = serviceConfig.classImageColorMap;
            this.id = n.id;
            this.ID = n.ID;
            this.number = n.number;
            //   this.number = Math.round(Math.random() * 2000);
            this._label = n.name || '';
            this.shape = n.shape || 'image';
            this.size = n.size || 16;
            // this.size = linearScale(this.number);
            this.type = types[n['class']];
            this.image = imagePath + this.type.image + '.inactive.' + this.type.extension;
            this.active = false;
            this.class = n.class;
          };

          var Edge = function (e) {
            this.id = e.id;
            this.label = e.label;
            this.from = e.from;
            this.to = e.to;
            this.toNode = data.nodes.getItemById(this.to);
            this.arrows = 'from';

            var color = scope.viewName.toLowerCase() === 'dag' ? '#f93' : this.toNode.type.headerColor;

            this.color = color;
            this.font = {
              size: 8,
              color: 'white',
              strokeWidth: 0,
              align: 'middle',
              background: 'rgba(0,0,0,0.8)',
            };
            if (e.duplicates >= 2) {
              this.smooth = {
                type: 'dynamic'
              };
            }
          };

          serviceSocket.connectStatus(function (response) {
            if (data.nodes) {
              var socketObj = JSON.parse(response);
              var messageObj = JSON.parse(socketObj.message);
              var maxVal = 0;
              _.each(scope.symbolClasses, function (symbolClass) {
                _.each(messageObj[symbolClass + 'Counter'], function (model) {
                  maxVal = Math.max(maxVal, model.In);
                });
              });
              var linearScale = d3.scaleLinear()
                .domain([0, maxVal])
                .range([10, 40])
                .interpolate(d3.interpolateRound);
              _.each(scope.symbolClasses, function (symbolClass) {
                _.each(messageObj[symbolClass + 'Counter'], function (model) {
                  if (model) {
                    var node = _.find(data.nodes._data, {ID: parseInt(model.Id)});
                    if (node) {
                      console.log(model.In, linearScale(model.In));
                      data.nodes.update([{id: node.id, number: model.In, size: linearScale(model.In)}]);
                      //node.update({number: model.In});
                      //node.number = model.In;
                    }
                  }
                });
              });
            }
          });

          $rootScope.$on('filterNodesChanged', function (event, filteredNode) {
            if (data.nodes) {
              //_.each(data.nodes._data, function (item) {
                //data.nodes._data.update([{id: filteredNode.id, hidden: filteredNode.visible}]);
              //});
              if(!filteredNode.isSearchText) {
                var selectedNodes = _.filter(data.nodes._data, {class: filteredNode.id.toLowerCase()});
                if (selectedNodes && selectedNodes.length > 0) {
                  _.each(selectedNodes, function (item) {
                    data.nodes.update([{id: item.id, hidden: !filteredNode.visible}]);
                    var selectedEdges = _.filter(data.edges._data, function (edge) {
                      return edge.from == item.id || edge.to == item.id;
                    });
                    if (selectedEdges && selectedEdges.length > 0) {
                      _.each(selectedEdges, function (edge) {
                        data.edges.update([{id: edge.id, hidden: !filteredNode.visible}]);
                      });
                    }
                  });
                }
              }else{

              }
              /*_.each(data.edges._data, function (item) {
                data.edges.update([{id: item.id, hidden: false}]);
              });*/

              //filterObj.filterList = _.orderBy(filterObj.filterList, ['checked'], ['desc']);
              /*_.each(filterObj.filterList, function (filterData) {
                var selectedNodes = _.filter(data.nodes._data, {class: filterData.name.toLowerCase()});
                if (selectedNodes && selectedNodes.length > 0) {
                  _.each(selectedNodes, function (item) {
                    //item.hidden = filterData.visible;
                    data.nodes.update([{id: item.id, hidden: !filterData.checked}]);
                    var selectedEdges = _.filter(data.edges._data, function (edge) {
                      return edge.from == item.id || edge.to == item.id;
                    });
                    if (selectedEdges && selectedEdges.length > 0) {
                      _.each(selectedEdges, function (edge) {
                        data.edges.update([{id: edge.id, hidden: !filterData.checked}]);
                      });
                    }
                  });
                }
              });*/
              /*if (filterObj.searchText != "") {
                var selectedNodes = _.filter(data.nodes._data, function (node) {
                  return !node._label.toLowerCase().contains(filterObj.searchText.toLowerCase())
                    && node.hidden == false;
                });

                _.each(selectedNodes, function (node) {
                  data.nodes.update([{id: node.id, hidden: true}]);
                  var selectedEdges = _.filter(data.edges._data, function (edge) {
                    return edge.from == node.id || edge.to == node.id;
                  });
                  if (selectedEdges && selectedEdges.length > 0) {
                    _.each(selectedEdges, function (edge) {
                      data.edges.update([{id: edge.id, hidden: true}]);
                    });
                  }
                });
              }*/
            }
          });

          (function fixNodeEdgePointerHover() {
            network.on('hoverNode', function () {
              angular.element('html,body').css('cursor', 'pointer');
            });
            network.on('blurNode', function () {
              angular.element('html,body').css('cursor', 'default');
            });
            network.on('hoverEdge', function () {
              angular.element('html,body').css('cursor', 'pointer');
            });
            network.on('blurEdge', function () {
              angular.element('html,body').css('cursor', 'default');
            });
          }());
          (function clickDoubleClickFix() {
            var doubleClickTime = 0;
            var threshold = 200;

            function doOnClick(params) {
              var id = params.nodes[0];
              if (id) {
                scope.nodeClick(id);
              } else {
                id = params.edges[0];
                if (id) {
                  updateNodesImagesToBeInactive();
                  scope.edgeClick(id);
                }
              }
            }

            function doOnDoubleClick(params) {
              var id = params.nodes[0];
              if (id) {
                scope.nodeDoubleClick(id);
              }
            }

            function onClick(params) {
              var t0 = new Date();
              if (t0 - doubleClickTime > threshold) {
                $timeout(function () {
                  if (t0 - doubleClickTime > threshold) {
                    doOnClick(params);
                  }
                }, threshold);
              }
            }

            function onDoubleClick(params) {
              doubleClickTime = new Date();
              doOnDoubleClick(params);
            }

            network.on('click', onClick);
            network.on('doubleClick', onDoubleClick);
          }());
          network.on('click', function (params) {
            if (!params.edges.length && !params.nodes.length) {
              updateNodesImagesToBeInactive();
              scope.groundClick();
            }
          });
          (function imageManipulationFunctions() {
            isNodeActive = function (id) {
              return data.nodes.getItemById(id).active;
            };
            updateNodesImagesToBeInactive = function () {
              data.nodes.forEach(function (n) {
                data.nodes.update({
                  id: n.id,
                  image: serviceConfig.classImageColorPath + n.type.image + '.inactive.' + n.type.extension,
                  active: false,
                  size: 16
                });
              });
            };
            updateNodeToBeActive = function (id) {
              var node = data.nodes.getItemById(id);
              data.nodes.update({
                id: id,
                image: serviceConfig.classImageColorPath + node.type.image + '.active.' + node.type.extension,
                active: true,
                size: 17
              });
              selectedNodes.add(node.id);
            };
            updateNodeToBeInactive = function (id) {
              var node = data.nodes.getItemById(id);
              data.nodes.update({
                id: id,
                image: serviceConfig.classImageColorPath + node.type.image + '.inactive.' + node.type.extension,
                active: false,
                size: 16
              });
              selectedNodes.delete(node.id);
            };
          }());
          scope.$on('closeSideMenu', function () {
            updateNodesImagesToBeInactive();
          });
          network.on('click', function (params) {
            var id = params.nodes[0];
            if (id) {
              var alreadyActive = isNodeActive(id);
              // updateNodesImagesToBeInactive();
              if (!alreadyActive) {
                updateNodeToBeActive(id);
              } else {
                updateNodeToBeInactive(id);
              }
            }
          });
          network.on('afterDrawing', function (ctx) {
            data.nodes.forEach(function (d) {
              if (d.hidden) {
                return;
              }
              var position = network.getPositions(d.id)[d.id];

              if (scope.showStatus === 'true') {

                ctx.textAlign = 'left';
                ctx.font = '9px arial';
                ctx.fillStyle = '#ffffff';
                ctx.fillText(d._label, (position.x + 13), (position.y - 4));
                ctx.font = '9px arial';
                ctx.fillStyle = '#FFCC00';
                ctx.fillText(d.number == undefined ? '' : d.number, (position.x + 19), (position.y - 15));
              } else {

                var rectWidth = d._label.length * 5 - 4;
                var rectHeight = 7;
                var cornerRadius = 5;


                // Opera 8.0+
                var isOpera = (!!window.opr && !!opr.addons) || !!window.opera || navigator.userAgent.indexOf(' OPR/') >= 0;
                // Firefox 1.0+
                var isFirefox = typeof InstallTrigger !== 'undefined';
                // At least Safari 3+: "[object HTMLElementConstructor]"
                var isSafari = Object.prototype.toString.call(window.HTMLElement).indexOf('Constructor') > 0;
                // Internet Explorer 6-11
                var isIE = /*@cc_on!@*/false || !!document.documentMode;
                // Edge 20+
                var isEdge = !isIE && !!window.StyleMedia;
                // Chrome 1+
                var isChrome = !!window.chrome && !!window.chrome.webstore;
                // Blink engine detection
                var isBlink = (isChrome || isOpera) && !!window.CSS;


                ctx.lineJoin = "round";
                ctx.lineWidth = 10;
                ctx.strokeStyle = "rgba(0,0,0,0.5)";
                if (isSafari) {
                  ctx.strokeRect((position.x + 21), (position.y - 4), rectWidth - cornerRadius, rectHeight - cornerRadius);
                } else if (isEdge) {
                  ctx.strokeRect((position.x + 19), (position.y - 4), rectWidth - cornerRadius, rectHeight - cornerRadius);
                } else {
                  ctx.strokeRect((position.x + 17), (position.y - 3), rectWidth - cornerRadius, rectHeight - cornerRadius);
                }

                ctx.textAlign = 'left';
                ctx.font = '9px arial';
                ctx.fillStyle = '#ffffff';
                if (isSafari) {
                  ctx.fillText(d._label, (position.x + 17), (position.y - 4 ));
                } else if (isEdge) {
                  ctx.fillText(d._label, (position.x + 14), (position.y - 6 ));
                } else {
                  ctx.fillText(d._label, (position.x + 13), (position.y - 4 ));
                }
              }

            });
          });
          var resizeNetworkAndReposition = function () {
            var windowHeight = $window.outerHeight;
            if (windowHeight >= 768) {
              var height = windowHeight.toString() + 'px';
              network.setOptions({height: height});
              container.style.height = height;
            }
            var windowWidth = $window.innerWidth;
            if (windowWidth >= 1024) {
              var width = windowWidth.toString() + 'px';
              network.setOptions({width: width});
              container.style.width = width;
            }
            network.moveTo(
              {
                position: {x: 0, y: 0},
                scale: 1.5
              }
            );
          };
          $window.onresize = resizeNetworkAndReposition;
          scope.$watch('data', function () {
            data.nodes.removeAll();
            data.nodes.add((function () {
              var nodes = [];
              _.each(scope.data.nodes, function (n) {
                nodes.push(new Node(n));
              });
              return nodes;
            }()));
            data.edges.removeAll();
            data.edges.add((function () {
              var edges = [];
              _.each(scope.data.edges, function (e) {
                edges.push(new Edge(e));
              });
              return edges;
            }()));
            resizeNetworkAndReposition();
          });
        }
      };
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
// vis.DataSet.prototype.updateAll = function (updateObject) {
//   'use strict';
//   var self = this;
//   this.forEach(function (d) {
//     updateObject.id = d.id;
//     self.update(updateObject);
//   });
// };
}());
