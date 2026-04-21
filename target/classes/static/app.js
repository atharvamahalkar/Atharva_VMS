const askBtn = document.getElementById('askBtn');
const ingestBtn = document.getElementById('ingestBtn');
const responseArea = document.getElementById('response');
const questionInput = document.getElementById('question');

askBtn.addEventListener('click', async () => {
    const question = questionInput.value.trim();
    if (!question) {
        responseArea.textContent = 'Please enter a question first.';
        return;
    }

    responseArea.textContent = 'Loading answer...';
    try {
        const res = await fetch('/api/query', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ question })
        });

        const data = await res.json();
        if (!res.ok) {
            responseArea.textContent = data.answer || data.message || 'Request failed';
            return;
        }

        responseArea.textContent = data.answer || 'No answer returned.';
    } catch (error) {
        responseArea.textContent = 'Error: ' + error.message;
    }
});

ingestBtn.addEventListener('click', async () => {
    responseArea.textContent = 'Starting ingestion...';
    try {
        const res = await fetch('/api/ingest', { method: 'POST' });
        const data = await res.json();
        responseArea.textContent = data.message || 'Ingestion complete.';
    } catch (error) {
        responseArea.textContent = 'Error: ' + error.message;
    }
});
