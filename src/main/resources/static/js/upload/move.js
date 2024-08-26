const clearAllButton = document.createElement("button");
clearAllButton.classList.add("clear-all-button");
clearAllButton.innerText = "Clear All";
document.querySelector(".file-uploader").appendChild(clearAllButton);

clearAllButton.addEventListener("click", () => {
    const fileItems = document.querySelectorAll(".file-list .file-item");

    // Remove file items from the list
    fileItems.forEach((item) => {
        item.remove();
    });

    totalFiles = 0;
    completedFiles = 0;
    fileCompletedStatus.innerText = `${completedFiles} / ${totalFiles} files completed`;

    // Make an HTTP GET request to the Spring Boot endpoint
    fetch("/call-flask")
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.text(); // Parse the response as text
        })
        .then(data => {
            console.log("Response from Flask endpoint:", data);
        })
        .catch(error => {
            console.error("Error calling Flask service:", error);
        });
});
