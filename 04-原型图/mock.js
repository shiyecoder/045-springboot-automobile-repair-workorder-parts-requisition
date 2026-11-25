
// 原型交互：仅做演示，不连后端
document.querySelectorAll('[data-confirm]').forEach(function(b){
  b.addEventListener('click',function(e){e.preventDefault();
    if(confirm(b.dataset.confirm))alert('已提交（原型演示，不写库）');});
});
document.querySelectorAll('.tabs a').forEach(function(a){
  a.addEventListener('click',function(e){e.preventDefault();
    a.parentNode.querySelectorAll('a').forEach(x=>x.classList.remove('on'));a.classList.add('on');});
});
document.querySelectorAll('.pager .pg').forEach(function(a){
  a.addEventListener('click',function(){document.querySelectorAll('.pager .pg').forEach(x=>x.classList.remove('on'));
    a.classList.add('on');});
});
