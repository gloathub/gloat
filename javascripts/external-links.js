// Open external links in new tab
document.addEventListener('DOMContentLoaded', function() {
  var links = document.querySelectorAll('a[href^="http"]');
  links.forEach(function(link) {
    var href = link.getAttribute('href');
    // Check if it's an external link (not same domain)
    if (href && !href.startsWith(window.location.origin)) {
      link.setAttribute('target', '_blank');
      link.setAttribute('rel', 'noopener noreferrer');
    }
  });
});
