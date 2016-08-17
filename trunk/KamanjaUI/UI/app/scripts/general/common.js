'use strict';

/*$(window).click(function(event){
  if(event.target.className !== 'logo')
    $('.modelDetails').animate({'left': '-1000px'}, 'slow').removeClass('visible');
});*/

$(document).ready(function(){
  $(".topHeader").click(function(){
    $('.modelDetails').animate({'left': '-1000px'}, 'slow').removeClass('visible');
  });
});

function toggleModelDetails(show){
  if(show){
    $('.modelDetails').animate({'left': '0px'}, 'slow').addClass('visible');
  }else{
    $('.modelDetails').animate({'left': '-1000px'}, 'slow').removeClass('visible');
  }
}


$('.modelDetails').click(function(event){
  event.stopPropagation();
});

String.prototype.format = function (args) {
  var newStr = this;
  for (var key in args) {
    newStr = newStr.split('{' + key + '}').join(args[key]);
  }
  return newStr;
}

String.prototype.contains = function (it) {
  return this.indexOf(it) != -1;
};
