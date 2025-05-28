import tkinter as tk
from tkinter import filedialog, scrolledtext, messagebox
import matplotlib.pyplot as plt
from matplotlib.backends.backend_tkagg import FigureCanvasTkAgg
from dateutil import parser
import re
import pandas as pd
from datetime import datetime
from collections import Counter

class LogViewerApp:
    def __init__(self, root):
        self.root = root
        self.root.title("Log Viewer & Log Analytics")

        # Menu
        menu = tk.Menu(root)
        root.config(menu=menu)
        file_menu = tk.Menu(menu, tearoff=False)
        menu.add_cascade(label="File", menu=file_menu)
        file_menu.add_command(label="Open...", command=self.open_file)
        file_menu.add_separator()
        file_menu.add_command(label="Exit", command=root.quit)

        # Text area
        self.text_area = scrolledtext.ScrolledText(root, wrap=tk.WORD, font=("Consolas", 10), height=20)
        self.text_area.pack(expand=True, fill='both')

        # Buttons
        button_frame = tk.Frame(root)
        button_frame.pack(pady=5)

        tk.Button(button_frame, text="🚀 Functionality Events Plot", command=self.plot_functionality_events).pack(side=tk.LEFT, padx=5)

        # Log data
        self.log_lines = []

    def open_file(self):
        file_path = filedialog.askopenfilename(
            title="Open Log File",
            filetypes=[("Log files", "*.log *.txt"), ("All files", "*.*")]
        )
        if file_path:
            with open(file_path, 'r', encoding='utf-8', errors='ignore') as f:
                self.log_lines = f.readlines()
            self.text_area.delete('1.0', tk.END)
            self.text_area.insert(tk.END, ''.join(self.log_lines))
            self.root.title(f"Log Viewer - {file_path}")

    def plot_functionality_events(self):
        if not self.log_lines:
            return messagebox.showwarning("No File", "Please open a log file first.")

        all_events = []
        functionality_order = []
        
        for line in self.log_lines:
            timestamp_match = re.search(r'"@timestamp":"(.*?)"', line)
            if not timestamp_match:
                continue
            
            timestamp = parser.isoparse(timestamp_match.group(1))
            message = line # Use the full line for message to check for keywords

            start_match = re.search(r"START EXECUTION FUNCTIONALITY: ([\w\s]+) with version (\d+)", message)
            end_match = re.search(r"END EXECUTION FUNCTIONALITY: ([\w\s]+) with version (\d+)", message)
            abort_match = re.search(r"ABORT EXECUTION FUNCTIONALITY: ([\w\s]+) with version (\d+)", message)
            exception_match = re.search(r"EXCEPTION THROWN: ([\w\s]+) with version (\d+)", message)

            functionality_name = "N/A"
            version = "N/A"
            event_type = "UNKNOWN"

            if start_match:
                functionality_name = start_match.group(1).strip()
                version = start_match.group(2)
                event_type = "START"
                if f"{functionality_name}" not in functionality_order:
                    functionality_order.append(f"{functionality_name}")
            elif end_match:
                functionality_name = end_match.group(1).strip()
                version = end_match.group(2)
                event_type = "END"
                if f"{functionality_name}" not in functionality_order:
                    functionality_order.append(f"{functionality_name}")
            elif abort_match:
                functionality_name = abort_match.group(1).strip()
                version = abort_match.group(2)
                event_type = "ABORT"
                if f"{functionality_name}" not in functionality_order:
                    functionality_order.append(f"{functionality_name}")
            
            elif exception_match:
                functionality_name = exception_match.group(1).strip()
                version = exception_match.group(2)
                event_type = "EXCEPTION"
                if f"{functionality_name}" not in functionality_order:
                    functionality_order.append(f"{functionality_name}")

            

            if event_type != "UNKNOWN":
                display_name = f"{functionality_name}" if version != "N/A" else functionality_name
                all_events.append({
                    "timestamp": timestamp,
                    "event_type": event_type,
                    "functionality": display_name
                })

        if not all_events:
            return messagebox.showinfo("No Data", "No functionality events (START, END, EXCEPTION, ABORT) found.")

        df_events = pd.DataFrame(all_events)
        df_events['timestamp_numeric'] = df_events['timestamp'].apply(lambda x: x.timestamp()) # Convert datetime to numeric for plotting
        
        # Sort functionalities based on their first appearance or an arbitrary order
        unique_functionalities = df_events['functionality'].unique()
        functionality_to_y = {name: i for i, name in enumerate(sorted(unique_functionalities))}
        df_events['y_pos'] = df_events['functionality'].map(functionality_to_y)


        fig, ax = plt.subplots(figsize=(15, max(8, len(unique_functionalities) * 0.7)))

        colors = {
            "START": "green",
            "END": "blue",
            "EXCEPTION": "red",
            "ABORT": "orange"
        }
        markers = {
            "START": "o",
            "END": "s",
            "EXCEPTION": "X",
            "ABORT": "D"
        }

        for event_type, color in colors.items():
            subset = df_events[df_events['event_type'] == event_type]
            if not subset.empty:
                ax.scatter(
                    subset['timestamp'],
                    subset['y_pos'],
                    color=color,
                    marker=markers.get(event_type, 'o'),
                    label=event_type,
                    s=100, # size of markers
                    alpha=0.7
                )
        
        ax.set_title("Functionality Event Timeline: Start, End, Exception, Abort")
        ax.set_xlabel("Time")
        ax.set_ylabel("Functionality")
        ax.set_yticks([functionality_to_y[name] for name in sorted(unique_functionalities)])
        ax.set_yticklabels(sorted(unique_functionalities))
        ax.grid(True, axis='both', linestyle='--', alpha=0.6)
        ax.legend(title="Event Type")
        plt.xticks(rotation=45, ha='right')
        plt.tight_layout()

        self.show_plot(fig, "🚀 Functionality Events Plot")


    def show_plot(self, fig, title):
        plot_win = tk.Toplevel(self.root)
        plot_win.title(title)
        canvas = FigureCanvasTkAgg(fig, master=plot_win)
        canvas.draw()
        canvas.get_tk_widget().pack(fill='both', expand=True)

# Launch app
if __name__ == "__main__":
    root = tk.Tk()
    app = LogViewerApp(root)
    root.geometry("1100x700")
    root.mainloop()
