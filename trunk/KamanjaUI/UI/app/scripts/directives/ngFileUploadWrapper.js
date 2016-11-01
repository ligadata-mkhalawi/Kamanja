/**
 * Created by mmajali on 11/1/16.
 */
'use strict';
angular.module('networkApp')
  .directive('ngFileUploadWrapper', [function () {
    return {
      restrict: 'A',
      template:"" +
      "<div ngf-drop ngf-select " +
      "     ng-model = 'file' class='drop-box'" +
      "     ngf-change='change()($file,event)'" +
      "     ngf-drag-over-class='dragover' " +
      "     ng-required = '{{required}}'     " +
      "     ng-mouseover='fileUploadHover = true'     " +
      "     ng-mouseleave='fileUploadHover = false'     " +
      "     ngf-accept='{{accept}}'     " +
      "     ngf-pattern='{{accept}}'>" +
      "    <span >" +
      "        <i ng-show='!file.name' class='fa fa-upload' aria-hidden='true'></i>" +
      "    </span>" +
      "    <div ng-show='!file.name' class='ngfDragDescription'>" +
      "        Drag your file here or " +
      "        <span class='browseLink'>browse</span>" +
      "        <br>{{acceptDescription}}" +
      "    </div>" +
      "    <div ng-show = " +
      "                  'file.name' " +
      "         class='fileName'>" +
      "         {{file.name}}" +
      "    </div>" +
      "    <span ng-show='file.name' class='removeLink' " +
      "       ng-click='file = undefined;$event.preventDefault();$event.stopPropagation();'>Remove</span>" +
      "</div>" +
      "<div ngf-no-file-drop>File Drag/Drop is not supported for this browser</div>"
      ,
      scope: {
        file:"=",
        required:"@",
        accept:"@",
        change: "&",
        acceptDescription:"@"
      },
      link: function (scope, element, attrs) {
      }
    }
  }]);
