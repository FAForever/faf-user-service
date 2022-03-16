const background_images = [
    "https://content.faforever.com/images/background/background-aeon-uef.jpg",
    "https://content.faforever.com/images/background/background-aeon.jpg",
    "https://content.faforever.com/images/background/background-cybran.jpg",
    "https://content.faforever.com/images/background/background-seraphim.jpg",
    "https://content.faforever.com/images/background/background-uef.jpg",
]

function getRandomImage() {
    return background_images[Math.floor(Math.random() * Math.floor(background_images.length))];
}

document.addEventListener("DOMContentLoaded", function() {
    document.documentElement.style.setProperty('--background-image-url', "url(" + getRandomImage() + ")")
});
