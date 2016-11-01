/**
 * Created by mmajali on 10/31/16.
 */

angular.module('networkApp')
  .controller('addNewModelModalCtrl', ['$scope', '$uibModalInstance', 'serviceData',
  function($scope, $uibModalInstance, serviceData){
    $scope.listConfig = [];
    $scope.selectedConfig = "";
    $scope.selectedConfigName = "Select Config";
    $scope.state = "config";
    var language = '';
    $scope.$on('modal.closing', function (event, reason, closed) {
      if ($scope.uploading) {
        event.preventDefault();
      }
    });
    $scope.addMessage = function () {
      if (!$scope.file) {
        serviceBase.showErrorNotification("Message file is required.");
        return;
      }
      $scope.uploading = true;
      if ($scope.file && $scope.file.name) {
        $scope.upload($scope.file, function (response) {
          $scope.uploading = false;
          serviceBase.showSuccessNotification('Message File Uploaded Successfully');
          $timeout(function () {
            $scope.state = 'config';
            $scope.$apply();
          }, 400);
        }, function () {
          $scope.uploading = false;
        });
      } else {
      }
    };
    $scope.addConfig = function () {
      if (!$scope.file2) {
        serviceBase.showErrorNotification("Model Config file is required.");
        return;
      }
      if (!$scope.selectedConfig.length) {
        serviceBase.showErrorNotification("You have to select config");
        return;
      }
      $scope.uploading = true;
      if ($scope.file2 && $scope.file2.name) {
        $scope.upload2($scope.file2, function (response) {
            $scope.uploading = false;
            $timeout(function () {
              $scope.state = 'model';
              $scope.$apply();
            }, 400);
          }, function () {
            $scope.uploading = false;
          }
        );
      } else {

      }
    };
    $scope.addModel = function () {
      if (!$scope.file3) {
        serviceBase.showErrorNotification("Model file is required.");
        return;
      }
      if (!$scope.projectName) {
        serviceBase.showErrorNotification("You have to select project name");
        return;
      }
      try {
        language = $scope.file3.name.split('.')[1]
      } catch (ex) {
        language = '';
      }
      $scope.uploading = true;
      var newModel = {
        projectName: $scope.projectName,
        name: $scope.modelName,
        id: $scope.modelName,
        age: 0,
        status: 'Active',
        createdAt: Date.now(),
        description: '',
        tags: '',
        lastModel: new Array(61).fill({x: 0, y: 0}),
        language: language,
        configName: $scope.selectedConfig
      };
      async.waterfall([
        function (callback) {
          if ($scope.file3 && $scope.file3.name) {
            $scope.upload3($scope.file3, function (response) {
              var length = response.split('.').length;
              newModel.name = response.split('.')[length - 2];
              newModel.originalName = response;
              newModel.version = response.split('.')[response.split('.').length - 1];
              $scope.uploading = false;
              serviceData.getModelDetailedInfo(response, function(result){
                var modelInfo = JSON.parse(result.APIResults["Result Data"]);
                newModel.id = modelInfo.Model.ElementId;
                serviceData.addModelMetaData(newModel, function (response) {
                  newModel._id = response._id;
                  //newModel.createdAt = response.createdAt;
                  newModel.version = formatModelVersion(newModel.version);
                  addModel(newModel);// this comes from controller
                  callback(newModel);
                });
              });


            }, function(){
              $scope.uploading = false;
            });
          } else {
            callback(null);
          }
        }
      ], function (result) {
        $uibModalInstance.close(newModel);
      });
    };

    $scope.upload2 = function (file, callback, errorCallback) {
      if (!file.$error) {
        var fileReader = new FileReader();
        fileReader.readAsText(file);
        fileReader.onload = function (e) {
          var data = e.target.result;
          serviceData.addDefinition(data, function (response) {
            if (response) {
              if (response.APIResults["Status Code"] === -1) {
                serviceBase.showErrorNotification(response.APIResults["Result Description"]);
                callback(response.APIResults["Result Description"]);
                return ;
              }
              if (response.APIResults["Status Code"] === 0) {
                callback('Upload 2');
                return;
              }
              errorCallback();
            }
            else {
              errorCallback();
            }
          });
        };
      }
    };
    $scope.upload3 = function (file, callback, errorCallback) {
      if (!file.$error) {
        var fileReader = new FileReader();
        fileReader.readAsText(file);
        fileReader.onload = function (e) {
          var data = e.target.result;
          var fileType = file.name.split('.')[1];
          if (fileType === 'xml') {
            serviceData.addPmml(data, fileType,{modelconfig:"system.DecisionTreeIris,0.1.0,system.irismsg"}, function (response) {
              if (response) {
                if (response.APIResults["Status Code"] === -1) {
                  serviceBase.showErrorNotification('Add Pmml', response.APIResults["Result Description"]);
                  errorCallback();
                  return;
                }
                if (response.APIResults["Status Code"] === 0) {

                  callback({isServerCall: true});
                  return;
                }
                errorCallback();
              }
              else {
                errorCallback();
              }
            });
          } else {
            serviceData.addModel(data, fileType, {modelconfig:$scope.selectedConfig, tenantid:"tenant1"}, function (response) {
              if (response) {
                if (response.APIResults["Status Code"] === -1) {
                  errorCallback();
                  return;
                }
                if (response.APIResults["Status Code"] === 0) {
                  callback(response.APIResults["Result Description"].split(':')[1]);
                  return;
                }
                errorCallback();
              }
              else {
                errorCallback();
              }
            });
          }
        };
      }
    };
    $scope.setProjectId = function ($event, project) {
      $event.preventDefault();
      $scope.projectName = project;
      $scope.dropDownContent = project;
    };
    $scope.dropDownContent = "Please select the project to include this model in";
    $scope.closeModal = function () {
      $uibModalInstance.dismiss('Cancel');
    };
    $scope.setConfigList = function (file, event) {
      var reader = new FileReader();
      reader.onload = function (e) {
        $scope.listConfig = [];
        var obj = JSON.parse(e.target.result);
        for (var propertyName in obj) {
          $scope.listConfig.push(propertyName);
        }
      };
      reader.readAsText(file);
    };
    $scope.setSelectedConfig = function (event, configObj) {
      $scope.selectedConfig = configObj;
      $scope.selectedConfigName = configObj;
    }
  }]);
