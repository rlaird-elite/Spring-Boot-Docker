import os
from pathlib import Path

# --- Configuration ---
# The name of the file this script will create.
OUTPUT_FILENAME = "combined_output.txt"
# ---------------------

def combine_files_in_directory():
    """
    Walks through the script's directory and all subdirectories,
    reads text files, and combines them into a single output file.
    """

    try:
        # Get the absolute path of the script itself.
        # We use .resolve() to get the full, unambiguous path.
        script_path = Path(__file__).resolve()
    except NameError:
        # Fallback for environments where __file__ is not defined (e.g., interactive)
        script_path = Path.cwd().resolve() / 'combine_files.py' # Assumed name

    # Get the directory containing the script.
    script_dir = script_path.parent

    # Define the full path for the output file.
    output_filepath = script_dir / OUTPUT_FILENAME

    print(f"Script directory: {script_dir}")
    print(f"Output file will be: {output_filepath}")

    # Track files to give a summary
    files_processed = 0
    files_skipped = 0

    try:
        # Open the output file in write mode with UTF-8 encoding.
        with open(output_filepath, 'w', encoding='utf-8') as outfile:

            # Use .rglob('*') to recursively find all items (files and dirs)
            for item_path in script_dir.rglob('*'):

                # We only care about files.
                if item_path.is_file():

                    # --- Critical Self-Preservation Check ---
                    # 1. Skip the script itself.
                    # 2. Skip the output file we are currently writing to.
                    if item_path == script_path or item_path == output_filepath:
                        print(f"Skipping: {item_path.relative_to(script_dir)} (Script/Output)")
                        continue

                    # Get the relative path for the header.
                    # This creates the 'filename.txt' or 'subdir/filename.txt' format.
                    relative_path = item_path.relative_to(script_dir)

                    try:
                        # Try to read the file as text.
                        with open(item_path, 'r', encoding='utf-8') as infile:
                            content = infile.read()

                        # Write the header and content to the output file.
                        outfile.write("=" * 80 + "\n")
                        outfile.write(f"--- File: {relative_path} ---\n")
                        outfile.write("=" * 80 + "\n\n")
                        outfile.write(content)
                        outfile.write("\n\n")

                        print(f"Processed: {relative_path}")
                        files_processed += 1

                    except (UnicodeDecodeError, IOError) as e:
                        # This will fail for binary files (images, executables, etc.)
                        # or files with permission issues. We'll just skip them.
                        print(f"Skipping: {relative_path} (Binary or unreadable)")
                        files_skipped += 1

        print("\n" + "=" * 80)
        print("Done!")
        print(f"Successfully combined {files_processed} text file(s).")
        print(f"Skipped {files_skipped} binary or unreadable file(s).")
        print(f"Output saved to: {OUTPUT_FILENAME}")
        print("=" * 80)

    except IOError as e:
        print(f"Error: Could not write to output file {output_filepath}. {e}")
    except Exception as e:
        print(f"An unexpected error occurred: {e}")

if __name__ == "__main__":
    combine_files_in_directory()
