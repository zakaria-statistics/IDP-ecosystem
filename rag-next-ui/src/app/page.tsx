"use client";

import { useState, useRef } from "react";

const API_URL = process.env.RAG_API_URL || "http://localhost:8080";

export default function Home() {
  const [question, setQuestion] = useState("");
  const [answer, setAnswer] = useState("");
  const [loading, setLoading] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [uploadStatus, setUploadStatus] = useState("");
  const [selectedFile, setSelectedFile] = useState<string>("");
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleQuery = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!question.trim()) return;

    setLoading(true);
    try {
      const res = await fetch(`${API_URL}/api/rag/query`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ question }),
      });
      const data = await res.json();
      setAnswer(data.answer);
    } catch (err) {
      setAnswer("Error connecting to RAG service");
    } finally {
      setLoading(false);
    }
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    setSelectedFile(file ? file.name : "");
    setUploadStatus("");
  };

  const handleUpload = async () => {
    const file = fileInputRef.current?.files?.[0];
    if (!file) {
      setUploadStatus("Please select a file");
      return;
    }

    setUploading(true);
    setUploadStatus("Uploading and processing...");

    try {
      const formData = new FormData();
      formData.append("file", file);

      const res = await fetch(`${API_URL}/api/rag/upload`, {
        method: "POST",
        body: formData,
      });
      const data = await res.json();

      if (data.status === "success") {
        setUploadStatus(`Ingested "${data.filename}" into ${data.chunks} chunks`);
        setSelectedFile("");
        if (fileInputRef.current) fileInputRef.current.value = "";
      } else {
        setUploadStatus(`Error: ${data.message || "Upload failed"}`);
      }
    } catch (err) {
      setUploadStatus("Error connecting to RAG service");
    } finally {
      setUploading(false);
    }
  };

  return (
    <main style={{ maxWidth: "800px", margin: "0 auto", padding: "2rem" }}>
      <h1 style={{ marginBottom: "1.5rem" }}>RAG Assistant</h1>

      {/* Upload Section */}
      <div
        style={{
          padding: "1.25rem",
          marginBottom: "2rem",
          border: "2px dashed #ccc",
          borderRadius: "8px",
          backgroundColor: "#f8f9fa",
        }}
      >
        <h3 style={{ marginBottom: "1rem", color: "#333" }}>Upload Document</h3>
        <div style={{ display: "flex", gap: "0.75rem", alignItems: "center", flexWrap: "wrap" }}>
          <label
            style={{
              padding: "0.6rem 1.2rem",
              backgroundColor: "#fff",
              border: "1px solid #ced4da",
              borderRadius: "4px",
              cursor: "pointer",
              flex: 1,
              minWidth: "200px",
              textAlign: "center",
              color: selectedFile ? "#333" : "#6c757d",
            }}
          >
            {selectedFile || "Choose file (.txt, .md)"}
            <input
              type="file"
              ref={fileInputRef}
              accept=".txt,.md,.text"
              onChange={handleFileChange}
              style={{ display: "none" }}
            />
          </label>
          <button
            onClick={handleUpload}
            disabled={uploading || !selectedFile}
            style={{
              padding: "0.6rem 1.5rem",
              fontSize: "1rem",
              backgroundColor: uploading ? "#6c757d" : "#28a745",
              color: "white",
              border: "none",
              borderRadius: "4px",
              cursor: uploading || !selectedFile ? "not-allowed" : "pointer",
              opacity: !selectedFile ? 0.6 : 1,
            }}
          >
            {uploading ? "Processing..." : "Upload & Ingest"}
          </button>
        </div>
        {uploadStatus && (
          <p
            style={{
              marginTop: "0.75rem",
              padding: "0.5rem",
              borderRadius: "4px",
              fontSize: "0.9rem",
              backgroundColor: uploadStatus.includes("Error") ? "#f8d7da" : "#d4edda",
              color: uploadStatus.includes("Error") ? "#721c24" : "#155724",
            }}
          >
            {uploadStatus}
          </p>
        )}
      </div>

      {/* Query Section */}
      <form onSubmit={handleQuery} style={{ marginBottom: "2rem" }}>
        <input
          type="text"
          value={question}
          onChange={(e) => setQuestion(e.target.value)}
          placeholder="Ask a question about your documents..."
          style={{
            width: "100%",
            padding: "0.85rem",
            fontSize: "1rem",
            border: "1px solid #ced4da",
            borderRadius: "4px",
            marginBottom: "1rem",
          }}
        />
        <button
          type="submit"
          disabled={loading}
          style={{
            padding: "0.75rem 2rem",
            fontSize: "1rem",
            backgroundColor: loading ? "#6c757d" : "#0070f3",
            color: "white",
            border: "none",
            borderRadius: "4px",
            cursor: loading ? "wait" : "pointer",
          }}
        >
          {loading ? "Searching..." : "Ask"}
        </button>
      </form>

      {answer && (
        <div
          style={{
            padding: "1.25rem",
            backgroundColor: "#fff",
            border: "1px solid #dee2e6",
            borderRadius: "8px",
            whiteSpace: "pre-wrap",
          }}
        >
          <h3 style={{ marginBottom: "0.75rem", color: "#333" }}>Answer:</h3>
          <p style={{ lineHeight: 1.6 }}>{answer}</p>
        </div>
      )}
    </main>
  );
}
