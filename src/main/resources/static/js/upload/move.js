// JavaScript 파일의 맨 아래에 추가합니다.

const clearAllButton = document.createElement("button");
clearAllButton.classList.add("clear-all-button");
clearAllButton.innerText = "Clear All";
document.querySelector(".file-uploader").appendChild(clearAllButton);

clearAllButton.addEventListener("click", () => {
    const fileItems = document.querySelectorAll(".file-list .file-item");
    fileItems.forEach((item) => {
        item.remove();
    });
    totalFiles = 0;
    completedFiles = 0;
    fileCompletedStatus.innerText = `${completedFiles} / ${totalFiles} files completed`;
});
